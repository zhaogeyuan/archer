package com.atpex.archer.processor;

import com.atpex.archer.cache.api.Cache;
import com.atpex.archer.exception.CacheOperationException;
import com.atpex.archer.exception.FallbackException;
import com.atpex.archer.metadata.ObjectCacheMetadata;
import com.atpex.archer.operation.ObjectCacheOperation;
import com.atpex.archer.processor.api.AbstractProcessor;
import com.atpex.archer.processor.context.InvocationContext;
import com.atpex.archer.roots.ObjectComponent;
import com.atpex.archer.util.CommonUtils;
import com.atpex.archer.util.ReflectionUtil;
import com.atpex.archer.util.SpringElUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


/**
 * Object cache processor
 *
 * @param <V> cache value type
 * @author atpexgo.wu
 * @see ObjectCacheOperation
 * @see com.atpex.archer.annotation.Cache
 * @see com.atpex.archer.annotation.CacheMulti
 * @since 1.0.0
 */
public class ObjectProcessor<V> extends AbstractProcessor<ObjectCacheOperation<V>, V> implements ObjectComponent {

    private final static Logger logger = LoggerFactory.getLogger(ObjectProcessor.class);

    @Override
    public V get(InvocationContext context, ObjectCacheOperation<V> cacheOperation) {
        ObjectCacheMetadata metadata = cacheOperation.getMetadata();
        logger.debug("Get invocation context {}", context);
        String key = generateCacheKey(context, cacheOperation.getMetadata());
        Cache.Entry entry = metadata.getInvokeAnyway() ? null : cache.get(key, cacheOperation.getCacheEventCollector());
        if (entry == null) {
            // not cached or 'invokeAnyway' flag is true
            return loadAndPut(context, cacheOperation);
        } else if (entry.getValue() == null) {
            logger.debug("Cache hit, value is NULL");
            // cached but value is really set to NULL
            return null;
        }

        // try to deserialize
        V value = cacheOperation.getValueSerializer().looseDeserialize(entry.getValue());
        if (value == null) {
            return loadAndPut(context, cacheOperation);
        }

        logger.debug("Cache hit");
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
                // take place first, keep the order right
                resultEntryMap.put(context, null);
            } else {
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
            return reordered;
        }

        logger.debug("{} entity(ies) missed, but {} entity(ies) hit and loaded at once", missCount, keys.size() - missCount);
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

        public OrderedHolder(InvocationContext key, V object, Object order) {
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
