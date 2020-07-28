package com.atpex.archer.spring.autoconfigure;

import com.atpex.archer.CacheManager;
import com.atpex.archer.spring.autoconfigure.annotation.EnableArcher;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Config selector
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public class ConfigSelector implements ImportSelector {

    @Override
    public String[] selectImports(AnnotationMetadata importMetadata) {
        AnnotationAttributes enableMethodCache = AnnotationAttributes.fromMap(
                importMetadata.getAnnotationAttributes(EnableArcher.class.getName(), false));
        if (enableMethodCache != null) {
            CacheManager.Config.metricsEnabled = enableMethodCache.getBoolean("enableMetrics");
            CacheManager.Config.valueSerialization = enableMethodCache.getEnum("serialization");
        }
        return new String[]{};
    }
}
