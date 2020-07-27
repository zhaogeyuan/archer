package com.atpex.archer.components.preset;

import com.atpex.archer.components.KeyGenerator;
import com.atpex.archer.metadata.AbstractCacheMetadata;

/**
 * Internal key generator
 *
 * @author atpexgo.wu
 * @since 1.0.0
 */
public class InternalKeyGenerator extends AbstractInternalKeyGenerator {

    private final DefaultKeyGenerator defaultKeyGenerator = new DefaultKeyGenerator();

    @Override
    String getMetaKeyGeneratorName(AbstractCacheMetadata metadata) {
        return metadata.getKeyGenerator();
    }

    @Override
    KeyGenerator defaultKeyGenerator() {
        return defaultKeyGenerator;
    }
}
