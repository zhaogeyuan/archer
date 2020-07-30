package com.github.attt.archer.components.api;

/**
 * Serializer
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public interface Serializer<R, S> {

    R deserialize(S serialized);

    S serialize(R raw);
}
