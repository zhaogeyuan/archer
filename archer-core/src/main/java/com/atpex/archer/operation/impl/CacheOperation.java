package com.atpex.archer.operation.impl;

import com.atpex.archer.loader.MultipleLoader;
import com.atpex.archer.loader.SingleLoader;
import com.atpex.archer.metadata.impl.CacheMetadata;
import com.atpex.archer.operation.AbstractCacheOperation;

/**
 * Abstract cache acceptation operation source
 *
 * @param <M> metadata type
 * @param <V> cache value type
 * @author atpexgo.wu
 * @see CacheMetadata
 * @since 1.0.0
 */
public abstract class CacheOperation<M extends CacheMetadata, V> extends AbstractCacheOperation<M> {

    protected SingleLoader<V> loader;

    protected MultipleLoader<V> multipleLoader;

    public SingleLoader<V> getLoader() {
        return loader;
    }

    public void setLoader(SingleLoader<V> loader) {
        this.loader = loader;
    }

    public MultipleLoader<V> getMultipleLoader() {
        return multipleLoader;
    }

    public void setMultipleLoader(MultipleLoader<V> multipleLoader) {
        this.multipleLoader = multipleLoader;
    }

}
