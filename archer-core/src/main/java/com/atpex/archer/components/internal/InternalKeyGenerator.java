package com.atpex.archer.components.internal;

import com.atpex.archer.components.api.KeyGenerator;
import com.atpex.archer.components.preset.DefaultKeyGenerator;
import com.atpex.archer.metadata.api.AbstractCacheMetadata;

/**
 * Internal key generator
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public class InternalKeyGenerator extends AbstractInternalKeyGenerator {

    private final DefaultKeyGenerator defaultKeyGenerator = new DefaultKeyGenerator();

    @Override
    public String getMetaKeyGeneratorName(AbstractCacheMetadata metadata) {
        return metadata.getKeyGenerator();
    }

    @Override
    public KeyGenerator defaultKeyGenerator() {
        return defaultKeyGenerator;
    }
}
