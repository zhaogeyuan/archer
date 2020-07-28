package com.atpex.archer.components.preset;

import com.atpex.archer.components.api.KeyGenerator;
import com.atpex.archer.metadata.api.AbstractCacheMetadata;
import com.atpex.archer.util.ReflectionUtil;

import java.lang.reflect.Method;

/**
 * Exclusive key generator
 *
 * @author atpexgo.wu
 * @since 1.0.0
 */
public class ExclusiveKeyGenerator implements KeyGenerator {

    @Override
    public String generateKey(AbstractCacheMetadata metadata, Object target, Method method, Object[] args, Object result, Object resultElement) {
        return ReflectionUtil.getSignatureWithArgValuesAndReturnType(method, args);
    }

}
