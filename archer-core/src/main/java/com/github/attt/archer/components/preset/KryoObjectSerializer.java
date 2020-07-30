package com.github.attt.archer.components.preset;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.github.attt.archer.components.api.ValueSerializer;
import com.github.attt.archer.util.ReflectionUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.function.Supplier;

/**
 * @author atpexgo.wu
 * @since 1.0
 */
public class KryoObjectSerializer<T> implements ValueSerializer<T> {

    private final ThreadLocal<Kryo> kryos = ThreadLocal.withInitial(new Supplier<Kryo>() {
        @Override
        public Kryo get() {
            Kryo kryo = new Kryo();
            kryo.setReferences(false);
            kryo.register(type);
            return kryo;
        }
    });

    private Class<?> type;

    public KryoObjectSerializer() {
    }

    public KryoObjectSerializer(Type type) {
        this.type = ReflectionUtil.toClass(type);
    }

    @Override
    public T deserialize(byte[] serialized) {
        Kryo kryo = kryos.get();

        try (Input input = new Input(serialized)) {
            return (T) kryo.readClassAndObject(input);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] serialize(T raw) {
        Kryo kryo = kryos.get();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             Output output = new Output(baos)) {
            kryo.writeClassAndObject(output, raw);
            output.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
