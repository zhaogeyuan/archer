package com.atpex.archer.components.preset;

import com.atpex.archer.components.ValueSerializer;
import com.atpex.archer.exception.CacheOperationException;

import java.io.*;

/**
 * Java object serializer
 *
 * @author atpexgo.wu
 * @since 1.0.0
 */
public class JavaObjectSerializer<T> implements ValueSerializer<T> {

    @Override
    public T deserialize(byte[] serialized) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(serialized);
             ObjectInput in = new ObjectInputStream(bis);) {
            return (T) in.readObject();
        } catch (Exception e) {
            throw new CacheOperationException(e);
        }
    }

    @Override
    public byte[] serialize(T raw) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutput out = new ObjectOutputStream(bos)) {
            out.writeObject(raw);
            return bos.toByteArray();
        } catch (Exception e) {
            throw new CacheOperationException(e);
        }
    }
}
