package com.atpex.archer.metadata.impl;

import com.atpex.archer.annotation.Cache;
import com.atpex.archer.annotation.CacheMulti;

/**
 * {@link Cache},{@link CacheMulti} cache metadata
 *
 * @author atpexgo.wu
 * @see Cache
 * @see CacheMulti
 * @since 1.0.0
 */
public class ObjectCacheMetadata extends CacheMetadata {

    private String valueSerializer;

    private boolean multiple;

    private String orderBy;

    public String getValueSerializer() {
        return valueSerializer;
    }

    public void setValueSerializer(String valueSerializer) {
        this.valueSerializer = valueSerializer;
    }

    public boolean isMultiple() {
        return multiple;
    }

    public void setMultiple(boolean multiple) {
        this.multiple = multiple;
    }

    public String getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(String orderBy) {
        this.orderBy = orderBy;
    }

    @Override
    public String toString() {
        return "ObjectCacheMetadata{" +
                "valueSerializer='" + valueSerializer + '\'' +
                ", multiple=" + multiple +
                ", orderBy='" + orderBy + '\'' +
                ", expirationInMillis=" + expirationInMillis +
                ", breakdownProtect=" + breakdownProtect +
                ", breakdownProtectTimeoutInMillis=" + breakdownProtectTimeoutInMillis +
                ", invokeAnyway=" + invokeAnyway +
                ", cacheAnnotation=" + cacheAnnotation +
                ", methodSignature='" + methodSignature + '\'' +
                ", area='" + area + '\'' +
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
