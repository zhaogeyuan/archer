package com.atpex.archer.components.preset;

import com.atpex.archer.components.api.KeyGenerator;
import com.atpex.archer.exception.CacheOperationException;
import com.atpex.archer.metadata.api.AbstractCacheMetadata;
import com.atpex.archer.util.CommonUtils;
import com.atpex.archer.util.SpringElUtil;

import java.lang.reflect.Method;

import static com.atpex.archer.constants.Constants.DEFAULT_DELIMITER;

/**
 * Default key generator
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public class DefaultKeyGenerator implements KeyGenerator {

    @Override
    public String generateKey(AbstractCacheMetadata metadata, Object target, Method method, Object[] args, Object result, Object resultElement) {

        String prefix = metadata.getKeyPrefix();
        String key = metadata.getKey();

        if (CommonUtils.isEmpty(key)) {
            throw new CacheOperationException("No cache key provided!");
        }
        Object value = SpringElUtil.parse(key).setMethodInvocationContext(target, method, args, result).addVar("result$each", resultElement).getValue();
        String cacheKey = String.valueOf(value);

        return CommonUtils.isEmpty(prefix) ? cacheKey : prefix + DEFAULT_DELIMITER + cacheKey;
    }

}
