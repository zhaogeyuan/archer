package com.atpex.archer.operation.impl;

import com.atpex.archer.metadata.impl.EvictionMetadata;
import com.atpex.archer.operation.AbstractCacheOperation;

/**
 * Cache eviction operation
 *
 * @author atpexgo.wu
 * @since 1.0.0
 */
public class EvictionOperation extends AbstractCacheOperation<EvictionMetadata> {

    @Override
    public String toString() {
        return "CacheEvictionOperationSource{" +
                "metadata=" + metadata +
                '}';
    }

    @Override
    public String initializedInfo() {
        return toString();
    }
}
