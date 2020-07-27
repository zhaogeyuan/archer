package com.atpex.archer.operation;

import com.atpex.archer.Component;
import com.atpex.archer.metadata.AbstractCacheMetadata;

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

    public M getMetadata() {
        return metadata;
    }

    public void setMetadata(M metadata) {
        this.metadata = metadata;
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
