package com.atpex.archer.operation.api;

import com.atpex.archer.roots.Component;
import com.atpex.archer.metadata.api.AbstractCacheMetadata;
import com.atpex.archer.stats.event.api.CacheEventCollector;

import static com.atpex.archer.constants.Constants.DEFAULT_DELIMITER;

/**
 * Abstract cache operation
 *
 * @author atpexgo.wu
 * @since 1.0.0
 */
public abstract class AbstractCacheOperation<M extends AbstractCacheMetadata> implements Component {

    private String cacheType;

    protected M metadata;

    protected CacheEventCollector cacheEventCollector;

    public M getMetadata() {
        return metadata;
    }

    public void setMetadata(M metadata) {
        this.metadata = metadata;
    }

    public CacheEventCollector getCacheEventCollector() {
        return cacheEventCollector;
    }

    public void setCacheEventCollector(CacheEventCollector cacheEventCollector) {
        this.cacheEventCollector = cacheEventCollector;
    }

    public String getCacheType() {
        if (cacheType == null) {
            String methodSignature = getMetadata().getMethodSignature();
            String condition = getMetadata().getCondition();
            cacheType = methodSignature + " " + DEFAULT_DELIMITER
                    + condition + " ";
        }
        return cacheType;
    }
}
