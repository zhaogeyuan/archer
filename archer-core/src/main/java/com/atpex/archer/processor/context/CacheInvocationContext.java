package com.atpex.archer.processor.context;

import com.atpex.archer.metadata.impl.CacheMetadata;
import com.atpex.archer.metrics.event.CacheHitRateEvent;
import com.atpex.archer.operation.impl.CacheOperation;

/**
 * Cache context
 *
 * @author atpexgo
 * @since 1.0.0
 */
public class CacheInvocationContext<V> {

    private String cacheKey;

    private String elementCacheKey;

    private CacheMetadata cacheMetadata;

    private CacheOperation<CacheMetadata, V> cacheOperation;

    private CacheHitRateEvent.CacheHitRateEventBuilder eventBuilder;

    public String getCacheKey() {
        return cacheKey;
    }

    public void setCacheKey(String cacheKey) {
        this.cacheKey = cacheKey;
    }

    public String getElementCacheKey() {
        return elementCacheKey;
    }

    public void setElementCacheKey(String elementCacheKey) {
        this.elementCacheKey = elementCacheKey;
    }

    public CacheOperation<CacheMetadata, V> getCacheOperation() {
        return cacheOperation;
    }

    public void setCacheOperation(CacheOperation<CacheMetadata, V> cacheOperation) {
        this.cacheOperation = cacheOperation;
    }

    public CacheHitRateEvent.CacheHitRateEventBuilder getEventBuilder() {
        return eventBuilder;
    }

    public void setEventBuilder(CacheHitRateEvent.CacheHitRateEventBuilder eventBuilder) {
        this.eventBuilder = eventBuilder;
    }

    public CacheMetadata getCacheMetadata() {
        return cacheMetadata;
    }

    public void setCacheMetadata(CacheMetadata cacheMetadata) {
        this.cacheMetadata = cacheMetadata;
    }
}
