package com.atpex.archer.metadata;

import com.atpex.archer.annotation.Evict;
import com.atpex.archer.metadata.api.AbstractCacheMetadata;

/**
 * {@link Evict} annotation metadata
 *
 * @author atpexgo.wu
 * @see Evict
 * @since 1.0
 */
public class EvictionMetadata extends AbstractCacheMetadata {

    private Boolean afterInvocation;

    private Boolean multiple;

    public Boolean getAfterInvocation() {
        return afterInvocation;
    }

    public void setAfterInvocation(Boolean afterInvocation) {
        this.afterInvocation = afterInvocation;
    }

    public Boolean getMultiple() {
        return multiple;
    }

    public void setMultiple(Boolean multiple) {
        this.multiple = multiple;
    }

    @Override
    public String toString() {
        return "EvictionMetadata{" +
                "afterInvocation=" + afterInvocation +
                ", multiple=" + multiple +
                ", cacheAnnotation=" + cacheAnnotation +
                ", methodSignature='" + methodSignature + '\'' +
                ", key='" + key + '\'' +
                ", keyPrefix='" + keyPrefix + '\'' +
                ", condition='" + condition + '\'' +
                ", keyGenerator='" + keyGenerator + '\'' +
                '}';
    }

    @Override
    public String initializedInfo() {
        return toString();
    }
}
