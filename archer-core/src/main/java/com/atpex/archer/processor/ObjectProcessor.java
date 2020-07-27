package com.atpex.archer.processor;

import com.himalaya.service.cacheable.cache.ObjectCacheComponent;
import com.himalaya.service.cacheable.cache.metadata.impl.ObjectCacheMetadata;
import com.himalaya.service.cacheable.cache.processor.ServiceCacheProcessor;
import com.himalaya.service.cacheable.cache.source.impl.ObjectOperationSource;
import com.himalaya.service.cacheable.context.CacheInvocationContext;
import com.himalaya.service.cacheable.context.CacheInvocationContexts;
import com.himalaya.service.cacheable.exceptions.CacheOperationException;
import com.himalaya.service.cacheable.exceptions.FallbackException;
import com.himalaya.service.cacheable.operator.CacheOperation;
import com.himalaya.service.cacheable.util.CommonUtils;
import com.himalaya.service.cacheable.util.ReflectionUtil;
import com.himalaya.service.cacheable.util.SpringElUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


/**
 * Object cache processor
 *
 * @param <V> cache value type
 * @author atpexgo.wu
 * @see ObjectOperationSource
 * @see com.himalaya.service.cacheable.annotation.Cacheable
 * @since 1.0.0
 */
public class ObjectProcessor<V> extends AbstractProcessor<V, ObjectOperationSource<V>> implements ServiceCacheProcessor<CacheInvocationContext, CacheInvocationContexts, V, ObjectOperationSource<V>>, ObjectCacheComponent {

    private final static Logger logger = LoggerFactory.getLogger(ObjectProcessor.class);

    @Override
    public V get(CacheInvocationContext context, ObjectOperationSource<V> source) {
        CacheOperation.StatsProcessor statsProcessor = () -> context.getEventBuilder().increaseQueryingTimes();
        ObjectCacheMetadata metadata = source.getMetadata();
        logger.debug("Get invocation context {}", context);
        String key = context.getCacheKey(keyGenerator, metadata);
        CacheOperation.Entry entry = metadata.getInvokeAnyway() ? null : cacheOperation.get(key, statsProcessor);
        if (entry == null) {
            // not cached or 'invokeAnyway' flag is true
            return loadAndPut(context, source);
        } else if (entry.getValue() == null) {
            context.getEventBuilder().increaseHitDataSize();
            logger.debug("Cache hit, value is NULL");
            // cached but value is really set to NULL
            return null;
        }
        context.getEventBuilder().increaseHitDataSize();

        // try to deserialize
        V value = source.getValueSerializer().looseDeserialize(entry.getValue());
        if (value == null) {
            return loadAndPut(context, source);
        }

        logger.debug("Cache hit");
        return value;
    }

    @Override
    public Map<? extends CacheInvocationContext, ? extends V> getAll(CacheInvocationContexts contexts, ObjectOperationSource<V> source) {
        CacheOperation.StatsProcessor statsProcessor = () -> contexts.getEventBuilder().increaseQueryingTimes();
        ObjectCacheMetadata metadata = source.getMetadata();

        List<CacheInvocationContext> invocationContextList = contexts.getCacheInvocationContextList();

        logger.debug("GetAll invocation context {}", Arrays.toString(invocationContextList.toArray()));
        Map<CacheInvocationContext, CacheOperation.Entry> resultEntryMap = new LinkedHashMap<>(invocationContextList.size());
        List<String> keys = new ArrayList<>();
        for (CacheInvocationContext context : invocationContextList) {
            keys.add(context.getCacheKey(keyGenerator, metadata));
        }

        List<CacheInvocationContext> contextList = new ArrayList<>(invocationContextList);
        Map<String, CacheOperation.Entry> entryMap = metadata.getInvokeAnyway() ? null : cacheOperation.getAll(keys, statsProcessor);

        if (CommonUtils.isEmpty(entryMap)) {
            logger.debug("No entity in cache found..., load all");
            CacheInvocationContexts invocationContexts = new CacheInvocationContexts();
            invocationContexts.setEventBuilder(contexts.getEventBuilder());
            invocationContexts.setCacheInvocationContextList(contextList);
            return loadAndPutAll(invocationContexts, source);
        }

        List<CacheInvocationContext> missedContextList = new ArrayList<>();

        int missCount = 0;

        // cache key order
        Map<CacheInvocationContext, V> deserializedValueCache = new HashMap<>();
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            CacheOperation.Entry entry = entryMap.get(key);
            CacheInvocationContext context = contextList.get(i);
            V deserializedValue = null;
            boolean isCacheMissing = entry == null;
            boolean isCacheNull = !isCacheMissing && entry.getValue() == null;
            boolean isCacheDeserializingFail = !isCacheMissing && !isCacheNull && (deserializedValue = source.getValueSerializer().looseDeserialize(entry.getValue())) == null;
            deserializedValueCache.put(context, deserializedValue);
            if (isCacheMissing || isCacheDeserializingFail) {
                missCount++;
                missedContextList.add(context);
                // take place first, keep the order right
                resultEntryMap.put(context, null);
            } else {
                contexts.getEventBuilder().increaseHitDataSize();
                resultEntryMap.put(context, entry);
            }
        }

        Map<CacheInvocationContext, V> loadedResult = new HashMap<>(missedContextList.size());

        if (!missedContextList.isEmpty()) {
            CacheInvocationContexts cacheInvocationContexts = new CacheInvocationContexts();
            cacheInvocationContexts.setEventBuilder(contexts.getEventBuilder());
            cacheInvocationContexts.setCacheInvocationContextList(missedContextList);
            loadedResult = loadAndPutAll(cacheInvocationContexts, source);
        }

        // if noise data exists, load method every time
        if (missCount > 0 && loadedResult.size() < missCount && loadedResult.containsKey(null)) {
            contexts.getEventBuilder().setPenetrated(true);
            // load from method;
            throw new FallbackException();
        }

        LinkedHashMap<CacheInvocationContext, V> combinedOrdered = new LinkedHashMap<>(invocationContextList.size());
        for (Map.Entry<CacheInvocationContext, CacheOperation.Entry> resultEntry : resultEntryMap.entrySet()) {
            if (!loadedResult.containsKey(resultEntry.getKey()) && missedContextList.contains(resultEntry.getKey())) {
                // absent in loaded result
                continue;
            }
            if (resultEntry.getValue() == null) {
                combinedOrdered.put(resultEntry.getKey(), loadedResult.get(resultEntry.getKey()));
            } else {
                combinedOrdered.put(resultEntry.getKey(), deserializedValueCache.get(resultEntry.getKey()));
            }
        }

        // need reorder ?
        if (!CommonUtils.isEmpty(metadata.getOrderBy())) {
            LinkedHashMap<CacheInvocationContext, V> reordered = new LinkedHashMap<>(invocationContextList.size());
            SpringElUtil.SpringELEvaluationContext springELEvaluationContext = SpringElUtil.parse(metadata.getOrderBy());
            Object[] objects = combinedOrdered.values().toArray();
            CacheInvocationContext[] invocationContexts = resultEntryMap.keySet().toArray(new CacheInvocationContext[0]);
            OrderedHolder<V>[] holders = new OrderedHolder[objects.length];
            for (int i = 0; i < objects.length; i++) {
                V object = (V) objects[i];
                CacheInvocationContext invocationContext = invocationContexts[i];
                Object order;
                if (object == null && metadata.getOrderBy().contains("#result")) {
                    order = Long.MAX_VALUE;
                } else {
                    order = springELEvaluationContext.setMethodInvocationContext(
                            invocationContext.getTarget(),
                            invocationContext.getMethod(),
                            invocationContext.getArgs(), null)
                            .addVar("result$each", object).getValue();
                    if (!ReflectionUtil.isNumber(order.getClass())) {
                        throw new CacheOperationException("Order result should be number type;");
                    }
                }
                holders[i] = new OrderedHolder<>(invocationContext, object, order);
            }
            Arrays.sort(holders);
            for (OrderedHolder<V> holder : holders) {
                reordered.put(holder.key, holder.object);
            }
            return reordered;
        }

        logger.debug("{} entity(ies) missed, but {} entity(ies) hit and loaded at once", missCount, keys.size() - missCount);
        return combinedOrdered;
    }

    static class OrderedHolder<V> implements Comparable<OrderedHolder<V>> {
        private final CacheInvocationContext key;
        private final V object;
        private final Object order;

        public OrderedHolder(CacheInvocationContext key, V object, Object order) {
            this.key = key;
            this.object = object;
            this.order = order;
        }

        @Override
        public int compareTo(OrderedHolder other) {
            return (int) (CommonUtils.parseNumber(String.valueOf(order), Long.class)
                    - CommonUtils.parseNumber(String.valueOf(other.order), Long.class));
        }
    }


    @Override
    public void put(CacheInvocationContext context, V value, ObjectOperationSource<V> source) {
        CacheOperation.StatsProcessor statsProcessor = () -> context.getEventBuilder().increaseQueryingTimes();
        ObjectCacheMetadata configAttribute = source.getMetadata();
        logger.debug("Put invocation context {}", context);
        String key = context.getCacheKey(keyGenerator, configAttribute);
        CacheOperation.Entry entry = cacheOperation.wrap(key, source.getValueSerializer().serialize(value), configAttribute.getExpirationInMillis());
        cacheOperation.put(key, entry, statsProcessor);
    }

    @Override
    public void putAll(CacheInvocationContexts contexts, Map<? extends CacheInvocationContext, ? extends V> contextMap, ObjectOperationSource<V> source) {
        CacheOperation.StatsProcessor statsProcessor = () -> contexts.getEventBuilder().increaseQueryingTimes();
        ObjectCacheMetadata configAttribute = source.getMetadata();
        Map<String, CacheOperation.Entry> kvMap = new HashMap<>(contextMap.size());
        for (Map.Entry<? extends CacheInvocationContext, ? extends V> contextEntry : contextMap.entrySet()) {
            if (contextEntry.getKey() == null) {
                // noise data
                continue;
            }
            String key = contextEntry.getKey().getCacheKey(keyGenerator, configAttribute);
            byte[] serializedValue = source.getValueSerializer().serialize(contextEntry.getValue());
            CacheOperation.Entry entry = cacheOperation.wrap(key, serializedValue, configAttribute.getExpirationInMillis());
            kvMap.put(key, entry);
        }
        if (CommonUtils.isNotEmpty(kvMap)) {
            cacheOperation.putAll(kvMap, statsProcessor);
        }
    }

}
