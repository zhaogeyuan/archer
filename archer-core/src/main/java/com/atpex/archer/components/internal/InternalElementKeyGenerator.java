package com.atpex.archer.components.internal;

import com.atpex.archer.components.api.KeyGenerator;
import com.atpex.archer.components.preset.DefaultElementKeyGenerator;
import com.atpex.archer.metadata.api.AbstractCacheMetadata;
import com.atpex.archer.metadata.ListCacheMetadata;

/**
 * Internal element key generator
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public class InternalElementKeyGenerator extends AbstractInternalKeyGenerator {

    private final DefaultElementKeyGenerator defaultKeyGenerator = new DefaultElementKeyGenerator();

    @Override
    public String getMetaKeyGeneratorName(AbstractCacheMetadata metadata) {
        ListCacheMetadata listableCacheMetadata = (ListCacheMetadata) metadata;
        return listableCacheMetadata.getElementKeyGenerator();
    }

    @Override
    public KeyGenerator defaultKeyGenerator() {
        return defaultKeyGenerator;
    }
}
