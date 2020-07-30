package com.github.attt.archer.test.components;

import com.github.attt.archer.components.api.KeyGenerator;
import com.github.attt.archer.metadata.api.AbstractCacheMetadata;

import java.lang.reflect.Method;

/**
 * @author atpex
 * @since 1.0
 */
public class CustomKeyGenerator implements KeyGenerator {
    @Override
    public String generateKey(AbstractCacheMetadata metadata, Object target, Method method, Object[] args, Object result, Object resultElement) {
        return null;
    }
}
