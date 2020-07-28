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


    public Boolean getAfterInvocation() {
        return afterInvocation;
    }

    public void setAfterInvocation(Boolean afterInvocation) {
        this.afterInvocation = afterInvocation;
    }

    @Override
    public String toString() {
        return "CacheEvictionMetadata{" +
                "afterInvocation=" + afterInvocation +
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
