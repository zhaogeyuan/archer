package com.atpex.archer.operation;


import com.atpex.archer.components.api.ValueSerializer;
import com.atpex.archer.metadata.ObjectCacheMetadata;
import com.atpex.archer.roots.ObjectComponent;

/**
 * Object cache operation
 *
 * @param <V> cache value type
 * @author atpexgo.wu
 * @since 1.0.0
 */
public class ObjectCacheOperation<V> extends CacheOperation<ObjectCacheMetadata, V> implements ObjectComponent {

    private ValueSerializer<V> valueSerializer;

    @Override
    public String toString() {
        return "ObjectCacheOperationSource{" +
                "valueSerializer=" + valueSerializer +
                ", metadata=" + metadata +
                '}';
    }

    public ValueSerializer<V> getValueSerializer() {
        return valueSerializer;
    }

    public void setValueSerializer(ValueSerializer<V> valueSerializer) {
        this.valueSerializer = valueSerializer;
    }

    @Override
    public String initializedInfo() {
        return toString();
    }
}
