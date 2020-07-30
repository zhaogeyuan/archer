package com.github.attt.archer.processor;

import com.github.attt.archer.annotation.CacheMulti;
import com.github.attt.archer.cache.api.Cache;
import com.github.attt.archer.exception.CacheOperationException;
import com.github.attt.archer.exception.FallbackException;
import com.github.attt.archer.metadata.ObjectCacheMetadata;
import com.github.attt.archer.operation.ObjectCacheOperation;
import com.github.attt.archer.processor.api.AbstractProcessor;
import com.github.attt.archer.processor.context.InvocationContext;
import com.github.attt.archer.roots.ObjectComponent;
import com.github.attt.archer.stats.event.CacheHitEvent;
import com.github.attt.archer.stats.event.CacheMissEvent;
import com.github.attt.archer.stats.event.CachePenetrationProtectedEvent;
import com.github.attt.archer.util.CommonUtils;
import com.github.attt.archer.util.ReflectionUtil;
import com.github.attt.archer.util.SpringElUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


/**
 * Object cache processor
 *
 * @param <V> cache value type
 * @author atpexgo.wu
 * @see ObjectCacheOperation
 * @see Cache
 * @see CacheMulti
 * @since 1.0
 */
public class ObjectProcessor<V> extends AbstractProcessor<ObjectCacheOperation<V>, V> implements ObjectComponent {

    private final static Logger logger = LoggerFactory.getLogger(ObjectProcessor.class);

    @Override
    public V get(InvocationContext context, ObjectCacheOperation<V> cacheOperation) {
        ObjectCacheMetadata metadata = cacheOperation.getMetadata();
        logger.debug("Get invocation context {}", context);
        if (metadata.getInvokeAnyway()) {
            return loadAndPut(context, cacheOperation);
        }
        String key = generateCacheKey(context, cacheOperation.getMetadata());
        Cache.Entry entry = cache.get(key, cacheOperation.getCacheEventCollector());
        if (entry == null) {
            // cache is missing
            cacheOperation.getCacheEventCollector().collect(new CacheMissEvent());
            return loadAndPut(context, cacheOperation);
        } else if (entry.getValue() == null) {
            cacheOperation.getCacheEventCollector().collect(new CachePenetrationProtectedEvent());
            logger.debug("Cache hit, value is NULL");
            cacheOperation.getCacheEventCollector().collect(new CacheHitEvent());
            // cached but value is really set to NULL
            return null;
        }

        // try to deserialize
        V value = cacheOperation.getValueSerializer().looseDeserialize(entry.getValue());
        if (value == null) {
            return loadAndPut(context, cacheOperation);
        }

        logger.debug("Cache hit");
        cacheOperation.getCacheEventCollector().collect(new CacheHitEvent());
        return value;
    }

    @Override
    public Map<InvocationContext, V> getAll(List<InvocationContext> invocationContexts, ObjectCacheOperation<V> cacheOperation) {
        ObjectCacheMetadata metadata = cacheOperation.getMetadata();


        logger.debug("GetAll invocation context {}", Arrays.toString(invocationContexts.toArray()));
        Map<InvocationContext, Cache.Entry> resultEntryMap = new LinkedHashMap<>(invocationContexts.size());
        List<String> keys = new ArrayList<>();
        for (InvocationContext context : invocationContexts) {
            keys.add(generateCacheKey(context, cacheOperation.getMetadata()));
        }

        List<InvocationContext> contextList = new ArrayList<>(invocationContexts);
        Map<String, Cache.Entry> entryMap = metadata.getInvokeAnyway() ? null : cache.getAll(keys, cacheOperation.getCacheEventCollector());

        if (CommonUtils.isEmpty(entryMap)) {
            logger.debug("No entity in cache found..., load all");
            cacheOperation.getCacheEventCollector().collect(new CacheMissEvent());
            return loadAndPutAll(invocationContexts, cacheOperation);
        }

        List<InvocationContext> missedContextList = new ArrayList<>();

        int missCount = 0;

        // cache key order
        Map<InvocationContext, V> deserializedValueCache = new HashMap<>();
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            Cache.Entry entry = entryMap.get(key);
            InvocationContext context = contextList.get(i);
            V deserializedValue = null;
            boolean isCacheMissing = entry == null;
            boolean isCacheNull = !isCacheMissing && entry.getValue() == null;
            boolean isCacheDeserializingFail = !isCacheMissing && !isCacheNull && (deserializedValue = cacheOperation.getValueSerializer().looseDeserialize(entry.getValue())) == null;
            deserializedValueCache.put(context, deserializedValue);
            if (isCacheMissing || isCacheDeserializingFail) {
                missCount++;
                missedContextList.add(context);
                cacheOperation.getCacheEventCollector().collect(new CacheMissEvent());
                // take place first, keep the order right
                resultEntryMap.put(context, null);
            } else {
                cacheOperation.getCacheEventCollector().collect(new CacheHitEvent());
                resultEntryMap.put(context, entry);
            }
        }

        Map<InvocationContext, V> loadedResult = new HashMap<>(missedContextList.size());

        if (!missedContextList.isEmpty()) {
            loadedResult = loadAndPutAll(invocationContexts, cacheOperation);
        }

        // if noise data exists, load method every time
        if (missCount > 0 && loadedResult.size() < missCount && loadedResult.containsKey(null)) {
            // load from method;
            cacheOperation.getCacheEventCollector().collect(new CacheMissEvent());
            throw new FallbackException();
        }

        LinkedHashMap<InvocationContext, V> combinedOrdered = new LinkedHashMap<>(invocationContexts.size());
        for (Map.Entry<InvocationContext, Cache.Entry> resultEntry : resultEntryMap.entrySet()) {
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
            LinkedHashMap<InvocationContext, V> reordered = new LinkedHashMap<>(invocationContexts.size());
            SpringElUtil.SpringELEvaluationContext springELEvaluationContext = SpringElUtil.parse(metadata.getOrderBy());
            Object[] objects = combinedOrdered.values().toArray();
            InvocationContext[] contexts = resultEntryMap.keySet().toArray(new InvocationContext[0]);
            OrderedHolder<V>[] holders = new OrderedHolder[objects.length];
            for (int i = 0; i < objects.length; i++) {
                V object = (V) objects[i];
                InvocationContext invocationContext = contexts[i];
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
            combinedOrdered = reordered;
        }

        logger.debug("{} missed/{} hit", missCount, keys.size() - missCount);
        return combinedOrdered;
    }

    @Override
    public void put(InvocationContext context, V value, ObjectCacheOperation<V> cacheOperation) {
        ObjectCacheMetadata metadata = cacheOperation.getMetadata();
        logger.debug("Put invocation context {}", context);
        String key = generateCacheKey(context, cacheOperation.getMetadata());
        Cache.Entry entry = cache.wrap(key, cacheOperation.getValueSerializer().serialize(value), metadata.getExpirationInMillis());
        cache.put(key, entry, cacheOperation.getCacheEventCollector());
    }

    @Override
    public void putAll(Map<InvocationContext, V> contextValueMap, ObjectCacheOperation<V> cacheOperation) {
        ObjectCacheMetadata metadata = cacheOperation.getMetadata();
        Map<String, Cache.Entry> kvMap = new HashMap<>(contextValueMap.size());
        for (Map.Entry<? extends InvocationContext, ? extends V> contextEntry : contextValueMap.entrySet()) {
            if (contextEntry.getKey() == null) {
                // noise data
                continue;
            }
            String key = generateCacheKey(contextEntry.getKey(), cacheOperation.getMetadata());
            byte[] serializedValue = cacheOperation.getValueSerializer().serialize(contextEntry.getValue());
            Cache.Entry entry = cache.wrap(key, serializedValue, metadata.getExpirationInMillis());
            kvMap.put(key, entry);
        }
        if (CommonUtils.isNotEmpty(kvMap)) {
            cache.putAll(kvMap, cacheOperation.getCacheEventCollector());
        }
    }

    static class OrderedHolder<V> implements Comparable<OrderedHolder<V>> {
        private final InvocationContext key;
        private final V object;
        private final Object order;

        OrderedHolder(InvocationContext key, V object, Object order) {
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

}
