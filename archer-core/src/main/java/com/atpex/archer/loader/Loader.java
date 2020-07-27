package com.atpex.archer.loader;

/**
 * Loader
 *
 * @author atpexgo.wu
 * @since 1.0.0
 */
public interface Loader<K, V> {
    V load(K k);
}
