package com.atpex.archer.cache;

import com.atpex.archer.Component;
import com.atpex.archer.cache.api.Cache;
import com.atpex.archer.cache.api.CacheInitializer;
import com.atpex.archer.cache.api.CacheShard;
import com.atpex.archer.exception.CacheBeanParsingException;

import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Internal cache operation initialization initializer delegate
 *
 * @author atpexgo.wu
 * @since 1.0.0
 */
public class CacheInitializerDelegate implements CacheInitializer, Component {

    private TreeMap<Integer, CacheInitializer> cacheInitializerTree;

    private static final String INTERNAL_REDIS_INITIALIZER_CLASS = "com.atpex.archer.cache.internal.RedisCacheInitializer";
    private static final String INTERNAL_CAFFEINE_INITIALIZER_CLASS = "com.atpex.archer.cache.internal.CaffeineCacheInitializer";

    public CacheInitializerDelegate() {
        cacheInitializerTree = new TreeMap<>();
        registerInitializer(findClass(INTERNAL_REDIS_INITIALIZER_CLASS));
        registerInitializer(findClass(INTERNAL_CAFFEINE_INITIALIZER_CLASS));
    }

    @Override
    public Cache initial(CacheShard shard) throws Throwable {
        InternalInitializerEntry initializerEntry = new InternalInitializerEntry();
        while ((initializerEntry = delegate(initializerEntry.next)) != null) {
            Cache cache = initializerEntry.initializer.initial(shard);
            if (cache != null) {
                return cache;
            }
        }
        throw new CacheBeanParsingException("Can't resolve cache shard, present instance is " + shard.getClass().getName());
    }

    private InternalInitializerEntry delegate(int order) {
        NavigableMap<Integer, CacheInitializer> map = cacheInitializerTree.tailMap(order, true);
        Map.Entry<Integer, CacheInitializer> initializerEntry = map.firstEntry();
        if (initializerEntry == null) {
            return null;
        }
        InternalInitializerEntry entry = new InternalInitializerEntry();
        entry.initializer = initializerEntry.getValue();
        entry.next = order + 1;
        if (!entry.initializer.enabled()) {
            return delegate(entry.next);
        }
        return entry;
    }

    static class InternalInitializerEntry {
        int next;
        CacheInitializer initializer;
    }

    private Class<? extends CacheInitializer> findClass(String name) {
        try {
            return (Class<? extends CacheInitializer>) Thread.currentThread().getContextClassLoader().loadClass(name);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private void registerInitializer(Class<? extends CacheInitializer> cacheInitializerType) {
        if (cacheInitializerType == null) {
            return;
        }
        try {
            CacheInitializer initializer = cacheInitializerType.newInstance();
            cacheInitializerTree.put(initializer.order(), initializer);
        } catch (Throwable ignored) {
        }

    }
}
