package com.atpex.archer.processor;

import com.atpex.archer.annotation.Cache;
import com.atpex.archer.cache.internal.ShardingCache;
import com.atpex.archer.components.preset.InternalElementKeyGenerator;
import com.atpex.archer.components.preset.InternalKeyGenerator;
import com.atpex.archer.components.preset.InternalKeySerializer;
import com.atpex.archer.metadata.impl.CacheMetadata;
import com.atpex.archer.metrics.event.CacheEvent;
import com.atpex.archer.metrics.observer.CacheMetricsObserver;
import com.atpex.archer.processor.context.CacheInvocationContext;
import com.atpex.archer.processor.context.InvocationContext;
import com.himalaya.service.cacheable.cache.metadata.AbstractCacheAcceptationMetadata;
import com.himalaya.service.cacheable.cache.metadata.AbstractCacheMetadata;
import com.himalaya.service.cacheable.context.CacheInvocationContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;


/**
 * Abstract service cache processor
 *
 * @author atpexgo.wu
 * @since 1.0.0
 */
public abstract class AbstractProcessor<V> implements Processor<V> {

    private final static Logger logger = LoggerFactory.getLogger(AbstractProcessor.class);

    protected final InternalKeyGenerator keyGenerator = new InternalKeyGenerator();

    protected final InternalElementKeyGenerator elementKeyGenerator = new InternalElementKeyGenerator();

    protected final InternalKeySerializer serializer = new InternalKeySerializer();

    protected ShardingCache cacheOperation;

    protected CacheMetricsObserver<CacheEvent> cacheObserver;

//    protected CacheManagement cacheManagement;


    private volatile ConcurrentHashMap<Object, BreakdownProtectionLock<V>> loaderMap;

    private ConcurrentHashMap<Object, BreakdownProtectionLock<V>> initOrGetLoaderMap() {
        if (loaderMap == null) {
            synchronized (this) {
                if (loaderMap == null) {
                    loaderMap = new ConcurrentHashMap<>();
                }
            }
        }
        return loaderMap;
    }


    @Override
    public void delete(InvocationContext<CacheInvocationContext<V>> invocationContext) {
        CacheInvocationContext cacheInvocationContext = invocationContext.getCacheContext();
        cacheOperation.remove(cacheInvocationContext.getCacheKey(), () -> cacheInvocationContext.getEventBuilder().increaseQueryingTimes());
    }


    /**
     * It's tricky way for {@link com.atpex.archer.invocation.CacheContext} to remove
     * cache by key in string literal
     *
     * @param key
     */
    public void deleteWithKey(String key) {
        cacheOperation.remove(key, () -> {
        });
    }

    /**
     * Get result by invoking loader
     * <p>
     * If {@link Cache#breakdownProtect()}}
     * is true, only one of concurrent requests will
     * actually load result by invoking loader, other requests
     * will hold on until loading-request is done or timeout
     * {@link Cache#breakdownProtectTimeout()}
     *
     * @see com.atpex.archer.annotation.CacheMulti
     * @see com.atpex.archer.annotation.CacheList
     * @see Cache
     * @param context
     * @return
     */
    protected V loadAndPut(InvocationContext<CacheInvocationContext<V>> context) {
        CacheMetadata cacheMetadata = context.getCacheContext().getCacheMetadata();
        logger.debug("Cache miss");
        if (cacheMetadata.getBreakdownProtect()) {
            logger.debug("Breakdown protect is on, loadAndPut synchronized");
            // synchronized loadAndPut
            return synchronizedLoadAndPut(context);
        } else {
            logger.debug("Breakdown protect is off, loadAndPut");
            // loadAndPut
            return loadAndPut0(context);
        }
    }

    private V loadAndPut0(InvocationContext<CacheInvocationContext<V>> context) {
        V load = load(context);
        put(context, load);
        return load;
    }

    private V load(InvocationContext<CacheInvocationContext<V>> context) {
        context.getCacheContext().getEventBuilder().setBreakdown(true);
        return context.getCacheContext().getCacheOperation().getLoader().load(context);
    }


    private V synchronizedLoadAndPut(InvocationContext<CacheInvocationContext<V>> context) {
        return doSynchronizedLoadAndPut(generateLockKey(context.getCacheContext(), context.getCacheContext().getCacheMetadata()),
                context.getCacheContext().getCacheMetadata().getBreakdownProtectTimeoutInMillis(),
                () -> load(context),
                v -> put(context, v, source),
                () -> loadAndPut0(context, source));
    }

    private <VAL> VAL doSynchronizedLoadAndPut(String lockKey, long breakdownTimeout, Supplier<VAL> load, Consumer<VAL> put, Supplier<VAL> loadAndPut) {
        ConcurrentHashMap<Object, BreakdownProtectionLock<VAL>> loaderMap = initOrGetLoaderMap();
        while (true) {
            boolean[] created = new boolean[1];
            BreakdownProtectionLock<VAL> ll = loaderMap.computeIfAbsent(lockKey, (unusedKey) -> {
                created[0] = true;
                BreakdownProtectionLock<VAL> loadingLock = new BreakdownProtectionLock<>();
                loadingLock.signal = new CountDownLatch(1);
                loadingLock.loaderThread = Thread.currentThread();
                return loadingLock;
            });
            if (created[0] || ll.loaderThread == Thread.currentThread()) {
                try {
                    VAL loadedValue = load.get();
                    ll.success = true;
                    ll.value = loadedValue;
                    put.accept(loadedValue);
                    return loadedValue;
                } finally {
                    if (created[0]) {
                        ll.signal.countDown();
                        loaderMap.remove(lockKey);
                    }
                }
            } else {
                try {
                    if (breakdownTimeout <= 0) {
                        ll.signal.await();
                    } else {
                        boolean ok = ll.signal.await(breakdownTimeout, TimeUnit.MILLISECONDS);
                        if (!ok) {
                            logger.debug("loader wait timeout:" + breakdownTimeout);
                            return loadAndPut.get();
                        }
                    }
                } catch (InterruptedException e) {
                    logger.debug("loader wait interrupted");
                    return loadAndPut.get();
                }
                if (ll.success) {
                    return (VAL) ll.value;
                }
            }
        }
    }

    protected Map<CacheInvocationContext, V> loadAndPutAll(CacheInvocationContexts contexts, S source) {
        AbstractCacheAcceptationMetadata configAttribute = source.getMetadata();
        logger.debug("Cache miss");
        if (configAttribute.getPenetrationProtect()) {
            logger.debug("Penetration protect is on, loadAndPutAll synchronized");
            // synchronized loadAndPut
            return synchronizedLoadAndPutAll(contexts, source);
        } else {
            logger.debug("Penetration protect is off, loadAndPutAll");
            // loadAndPut
            return loadAndPutAll0(contexts, source);
        }
    }

    private Map<CacheInvocationContext, V> loadAll(CacheInvocationContexts contexts, S source) {
        // actually single loader and multiple loader won't be set at the same time
        // but in case, use multiple loader first
        if (source.getMultipleLoader() != null) {
            contexts.getEventBuilder().setPenetrated(true);
            return source.getMultipleLoader().load(contexts.getCacheInvocationContextList());
        }
        Map<CacheInvocationContext, V> result = new HashMap<>();
        for (CacheInvocationContext context : contexts.getCacheInvocationContextList()) {
            contexts.getEventBuilder().setPenetrated(true);
            result.put(context, source.getLoader().load(context));
        }
        return result;
    }

    private Map<CacheInvocationContext, V> loadAndPutAll0(CacheInvocationContexts contexts, S source) {
        Map<CacheInvocationContext, V> load = loadAll(contexts, source);
        putAll(contexts, load, source);
        return load;
    }

    private Map<CacheInvocationContext, V> synchronizedLoadAndPutAll(CacheInvocationContexts contexts, S source) {
        return doSynchronizedLoadAndPut(source, generateLockKey(contexts.getCacheInvocationContextList(), source.getMetadata()),
                () -> loadAll(contexts, source),
                v -> putAll(contexts, v, source),
                () -> loadAndPutAll0(contexts, source));
    }


    private String generateLockKey(List<CacheInvocationContext> contexts, AbstractCacheMetadata cacheAttribute) {
        StringBuilder stringBuilder = new StringBuilder();
        for (CacheInvocationContext context : contexts) {
            stringBuilder.append(context.getCacheKey(keyGenerator, cacheAttribute)).append("_");
        }
        return stringBuilder.toString();
    }

    private String generateLockKey(CacheInvocationContext context, AbstractCacheMetadata cacheAttribute) {
        return context.getCacheKey(keyGenerator, cacheAttribute);
    }


    public void afterCacheManagementSet() {
        cacheObserver = cacheManagement.getCacheObserver();
        cacheOperation = cacheManagement.getShardingCacheOperation();
        keyGenerator.setKeyGeneratorMap(cacheManagement.getKeyGeneratorMap());
        elementKeyGenerator.setKeyGeneratorMap(cacheManagement.getKeyGeneratorMap());
    }
}
