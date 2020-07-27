package com.atpex.archer.components.preset;

import com.atpex.archer.components.Serializer;

import java.nio.charset.StandardCharsets;

/**
 * Internal key serializer
 *
 * @author atpexgo.wu
 * @since 1.0.0
 */
public class InternalKeySerializer implements Serializer<String, byte[]> {


    @Override
    public String deserialize(byte[] serialized) {
        return new String(serialized, StandardCharsets.UTF_8);
    }

    @Override
    public byte[] serialize(String raw) {
        return raw.getBytes(StandardCharsets.UTF_8);
    }
}
