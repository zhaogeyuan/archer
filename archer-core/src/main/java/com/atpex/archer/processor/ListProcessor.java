package com.atpex.archer.processor;

import com.atpex.archer.cache.api.Cache;
import com.atpex.archer.metadata.ListCacheMetadata;
import com.atpex.archer.operation.ListCacheOperation;
import com.atpex.archer.processor.api.AbstractProcessor;
import com.atpex.archer.processor.context.InvocationContext;
import com.atpex.archer.roots.ListComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Abstract listable service cache processor
 *
 * @author atpexgo.wu
 * @since 1.0.0
 */
public class ListProcessor<V> extends AbstractProcessor<ListCacheOperation<V>, Collection<V>> implements ListComponent {

    private final static Logger logger = LoggerFactory.getLogger(ListProcessor.class);

    @Override
    public Collection<V> get(InvocationContext context, ListCacheOperation<V> cacheOperation) {
        ListCacheMetadata metadata = cacheOperation.getMetadata();
        logger.debug("Get invocation context {}", context);
        if (metadata.getInvokeAnyway()) {
            // 'invokeAnyway' is set to true
            return loadAndPut(context, cacheOperation);
        }

        // fetch all object cache keys
        List<String> allCacheKeys;
        String key = generateCacheKey(context, metadata);
        Cache.Entry entry = cache.get(key, cacheOperation.getCacheEventCollector());
        if (entry == null) {
            // cache is missing
            return loadAndPut(context, cacheOperation);
        } else if (entry.getValue() == null) {
            // cache value is null
            return null;
        } else {
            allCacheKeys = cacheOperation.getElementCacheKeySerializer().looseDeserialize(entry.getValue());
        }

        if (allCacheKeys == null) {
            // treat as cache is missing
            return loadAndPut(context, cacheOperation);
        }

        // fetch all object
        Map<String, Cache.Entry> allObjects = cache.getAll(allCacheKeys, cacheOperation.getCacheEventCollector());

        Map<String, V> resultMap = new HashMap<>(allCacheKeys.size());
        for (Map.Entry<String, Cache.Entry> cacheEntry : allObjects.entrySet()) {
            String cacheObjectKey = cacheEntry.getKey();
            Cache.Entry cacheObject = cacheEntry.getValue();
            if (cacheObject == null) {
                // treat as cache is missing
                return loadAndPut(context, cacheOperation);
            }
            V deserialized;
            if (cacheObject.getValue() != null) {
                deserialized = cacheOperation.getValueSerializer().looseDeserialize(cacheObject.getValue());
                if (deserialized == null) {
                    // deserialized failed, treat as cache is missing
                    return loadAndPut(context, cacheOperation);
                }
            } else {
                // cached object self is null, treat as null
                deserialized = null;
            }
            resultMap.put(cacheObjectKey, deserialized);
        }

        List<V> result = new ArrayList<>();
        // collect cache value in order
        for (String cacheKey : allCacheKeys) {
            result.add(resultMap.get(cacheKey));
        }

        return result;
    }

    @Override
    public Map<InvocationContext, Collection<V>> getAll(List<InvocationContext> contextList, ListCacheOperation<V> cacheOperation) {
        // may be more elegant? not to use looping
        Map<InvocationContext, Collection<V>> resultMap = new HashMap<>(contextList.size());
        for (InvocationContext context : contextList) {
            resultMap.put(context, get(context, cacheOperation));
        }
        return resultMap;
    }

    @Override
    public void put(InvocationContext context, Collection<V> values, ListCacheOperation<V> cacheOperation) {
        ListCacheMetadata metadata = cacheOperation.getMetadata();
        logger.debug("Put invocation context {}", context);

        String key = generateCacheKey(context, cacheOperation.getMetadata());

        if (values == null) {
            cache.put(key, cache.wrap(key, null, metadata.getExpirationInMillis()), cacheOperation.getCacheEventCollector());
        } else {
            List<String> elementCacheKeys = new ArrayList<>();
            Map<String, Cache.Entry> elements = new HashMap<>();
            for (V value : values) {
                String elementKey = generateElementCacheKey(context, cacheOperation.getMetadata(), values, value);
                elements.put(elementKey, cache.wrap(elementKey, cacheOperation.getValueSerializer().serialize(value), metadata.getExpirationInMillis()));
                elementCacheKeys.add(elementKey);
            }
            cache.putAllIfAbsent(elements, cacheOperation.getCacheEventCollector());
            cache.put(key, cache.wrap(key, cacheOperation.getElementCacheKeySerializer().serialize(elementCacheKeys), metadata.getExpirationInMillis()), cacheOperation.getCacheEventCollector());
        }
    }

    @Override
    public void putAll(Map<InvocationContext, Collection<V>> contextValueMap, ListCacheOperation<V> cacheOperation) {
// may be more elegant? not to use looping
        for (Map.Entry<? extends InvocationContext, ? extends Collection<V>> entry : contextValueMap.entrySet()) {
            put(entry.getKey(), entry.getValue(), cacheOperation);
        }
    }
}
