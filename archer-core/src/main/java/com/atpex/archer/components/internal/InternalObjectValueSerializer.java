package com.atpex.archer.components.internal;

import com.atpex.archer.CacheManager;
import com.atpex.archer.components.api.ValueSerializer;
import com.atpex.archer.components.preset.FastJsonObjectSerializer;
import com.atpex.archer.components.preset.HessianObjectSerializer;
import com.atpex.archer.components.preset.JavaObjectSerializer;
import com.atpex.archer.components.preset.KryoObjectSerializer;
import com.atpex.archer.constants.Serialization;
import com.atpex.archer.exception.CacheBeanParsingException;

import java.lang.reflect.Type;

/**
 * Internal object value serializer
 *
 * @author atpexgo.wu
 * @since 1.0.0
 */
public final class InternalObjectValueSerializer<V> implements ValueSerializer<V> {

    private final ValueSerializer<V> presetValueSerializer;


    public InternalObjectValueSerializer(Type type) {
        checkDependency(CacheManager.Config.valueSerialization);
        switch (CacheManager.Config.valueSerialization) {
            case FAST_JSON:
                this.presetValueSerializer = new FastJsonObjectSerializer<>(type);
                break;
            case KRYO:
                this.presetValueSerializer = new KryoObjectSerializer<>(type);
                break;
            case HESSIAN:
                this.presetValueSerializer = new HessianObjectSerializer<>();
                break;
            case JAVA:
            default:
                this.presetValueSerializer = new JavaObjectSerializer<>();
        }
    }

    @Override
    public V deserialize(byte[] serialized) {
        return presetValueSerializer.deserialize(serialized);
    }


    @Override
    public byte[] serialize(V raw) {
        return presetValueSerializer.serialize(raw);
    }

    public void checkDependency(Serialization serialization) {
        try {
            Class.forName(serialization.getMainDependency());
        } catch (ClassNotFoundException e) {
            throw new CacheBeanParsingException("Preset value serializer initialization error, maybe you forget to add " + serialization.name() + " serialization dependency?", e);
        }
    }
}
