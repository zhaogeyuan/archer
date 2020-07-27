package com.atpex.archer.components.preset;

import com.atpex.archer.components.KeyGenerator;
import com.atpex.archer.metadata.AbstractCacheMetadata;
import com.atpex.archer.metadata.impl.ListCacheMetadata;

/**
 * Internal element key generator
 *
 * @author atpexgo.wu
 * @since 1.0.0
 */
public class InternalElementKeyGenerator extends AbstractInternalKeyGenerator {

    private final DefaultElementKeyGenerator defaultKeyGenerator = new DefaultElementKeyGenerator();

    @Override
    String getMetaKeyGeneratorName(AbstractCacheMetadata metadata) {
        ListCacheMetadata listableCacheMetadata = (ListCacheMetadata) metadata;
        return listableCacheMetadata.getElementKeyGenerator();
    }

    @Override
    KeyGenerator defaultKeyGenerator() {
        return defaultKeyGenerator;
    }
}
