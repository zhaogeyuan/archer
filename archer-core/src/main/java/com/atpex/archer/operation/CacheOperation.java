package com.atpex.archer.operation;

import com.atpex.archer.loader.MultipleLoader;
import com.atpex.archer.loader.SingleLoader;
import com.atpex.archer.metadata.CacheMetadata;
import com.atpex.archer.operation.api.AbstractCacheOperation;

/**
 * Cache acceptation operation
 *
 * @param <M> metadata type
 * @param <V> cache value type
 * @author atpexgo.wu
 * @see CacheMetadata
 * @since 1.0.0
 */
public class CacheOperation<M extends CacheMetadata, V> extends AbstractCacheOperation<M> {

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