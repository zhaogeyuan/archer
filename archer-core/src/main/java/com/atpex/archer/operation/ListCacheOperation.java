package com.atpex.archer.operation;


import com.alibaba.fastjson.TypeReference;
import com.atpex.archer.components.api.ValueSerializer;
import com.atpex.archer.components.internal.InternalObjectValueSerializer;
import com.atpex.archer.metadata.ListCacheMetadata;
import com.atpex.archer.roots.ListComponent;

import java.util.Collection;
import java.util.List;

/**
 * Abstract listable cache operation
 *
 * @param <V> object cache value type
 * @author atpexgo.wu
 */
public class ListCacheOperation<V> extends CacheOperation<ListCacheMetadata, Collection<V>> implements ListComponent {

    private ValueSerializer<List<String>> elementCacheKeySerializer = new InternalObjectValueSerializer(new TypeReference<List<String>>() {
    }.getType());

    private ValueSerializer<V> valueSerializer;

    public ValueSerializer<List<String>> getElementCacheKeySerializer() {
        return elementCacheKeySerializer;
    }

    public void setElementCacheKeySerializer(ValueSerializer<List<String>> elementCacheKeySerializer) {
        this.elementCacheKeySerializer = elementCacheKeySerializer;
    }

    public ValueSerializer<V> getValueSerializer() {
        return valueSerializer;
    }

    public void setValueSerializer(ValueSerializer<V> valueSerializer) {
        this.valueSerializer = valueSerializer;
    }
}
