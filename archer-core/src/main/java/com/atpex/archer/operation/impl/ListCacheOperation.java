package com.atpex.archer.operation.impl;


import com.alibaba.fastjson.TypeReference;
import com.atpex.archer.components.ValueSerializer;
import com.atpex.archer.components.preset.InternalObjectValueSerializer;
import com.atpex.archer.metadata.impl.ListCacheMetadata;

import java.util.Collection;
import java.util.List;

/**
 * Abstract listable cache operation
 *
 * @param <OV> object cache value type
 * @author atpexgo.wu
 */
public class ListCacheOperation<OV> extends CacheOperation<ListCacheMetadata, Collection<OV>> {

    private ValueSerializer<List<String>> elementCacheKeySerializer = new InternalObjectValueSerializer(new TypeReference<List<String>>() {
    }.getType());

    private ValueSerializer<OV> valueSerializer;

    public ValueSerializer<List<String>> getElementCacheKeySerializer() {
        return elementCacheKeySerializer;
    }

    public void setElementCacheKeySerializer(ValueSerializer<List<String>> elementCacheKeySerializer) {
        this.elementCacheKeySerializer = elementCacheKeySerializer;
    }

    public ValueSerializer<OV> getValueSerializer() {
        return valueSerializer;
    }

    public void setValueSerializer(ValueSerializer<OV> valueSerializer) {
        this.valueSerializer = valueSerializer;
    }
}
