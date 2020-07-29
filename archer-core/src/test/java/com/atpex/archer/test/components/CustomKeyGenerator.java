package com.atpex.archer.test.components;

import com.atpex.archer.components.api.KeyGenerator;
import com.atpex.archer.metadata.api.AbstractCacheMetadata;

import java.lang.reflect.Method;

/**
 *
 * @author atpex
 * @since 1.0
 */
public class CustomKeyGenerator implements KeyGenerator {
    @Override
    public String generateKey(AbstractCacheMetadata metadata, Object target, Method method, Object[] args, Object result, Object resultElement) {
        return null;
    }
}
