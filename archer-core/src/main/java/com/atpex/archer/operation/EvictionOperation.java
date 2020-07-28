package com.atpex.archer.operation;

import com.atpex.archer.metadata.EvictionMetadata;
import com.atpex.archer.operation.api.AbstractCacheOperation;

/**
 * Cache eviction operation
 *
 * @author atpexgo.wu
 * @since 1.0
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
