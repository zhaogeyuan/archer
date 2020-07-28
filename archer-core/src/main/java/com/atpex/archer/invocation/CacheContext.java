package com.atpex.archer.invocation;

import com.atpex.archer.CacheManager;
import com.atpex.archer.processor.api.AbstractProcessor;
import com.atpex.archer.roots.ListComponent;
import com.atpex.archer.roots.ObjectComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service cacheable context
 *
 * @author atpexgo.wu
 * @since 1.0
 */
@SuppressWarnings("all")
public class CacheContext {

    private static final Logger logger = LoggerFactory.getLogger(CacheContext.class);

    private static final ThreadLocal<Object> currentProxy = new ThreadLocal<>();

    private static CacheManager cacheManager;

    private CacheContext() {
    }

    public static void setCacheManager(CacheManager cacheManager) {
        if (CacheContext.cacheManager == null) {
            CacheContext.cacheManager = cacheManager;
        }
    }

    public static Object currentProxy() throws IllegalStateException {
        return currentProxy.get();
    }

    public static <S> S currentProxy(S s) throws IllegalStateException {
        return (S) currentProxy.get();
    }

    static Object setCurrentProxy(Object proxy) {
        Object old = currentProxy.get();
        if (proxy != null) {
            currentProxy.set(proxy);
        } else {
            currentProxy.remove();
        }
        return old;
    }

    public static void evictList(String key) {
        AbstractProcessor processor = cacheManager.getProcessor(ListComponent.class);
        evict(key, processor);
    }

    public static void evictObject(String key) {
        AbstractProcessor processor = cacheManager.getProcessor(ObjectComponent.class);
        evict(key, processor);
    }

    private static void evict(String key, AbstractProcessor processor) {
        processor.deleteWithKey(key);
    }
}
