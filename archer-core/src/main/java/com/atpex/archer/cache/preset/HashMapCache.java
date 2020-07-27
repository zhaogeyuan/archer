package com.atpex.archer.cache.preset;

import com.atpex.archer.cache.api.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Hash map operation cache operation
 * Only for debugging
 *
 * @author atpexgo.wu
 * @since 1.0.0
 */
public class HashMapCache implements Cache {

    private static final Logger logger = LoggerFactory.getLogger(HashMapCache.class);

    private final Map<String, Entry> unsafe = new WeakHashMap<>();

    private final Map<String, Entry> map = Collections.synchronizedMap(unsafe);

    @Override
    public boolean containsKey(String key, StatsProcessor processor) {
        boolean exists = map.containsKey(key);
        processor.cacheAccessed();
        return exists;
    }

    @Override
    public Entry get(String key, StatsProcessor processor) {
        Entry entry = map.get(key);
        processor.cacheAccessed();
        return entry;
    }

    @Override
    public Map<String, Entry> getAll(Collection<String> keys, StatsProcessor processor) {
        if (keys == null) {
            return new HashMap<>(0);
        }
        Map<String, Entry> result = new HashMap<>(keys.size());
        for (String key : keys) {
            result.put(key, map.getOrDefault(key, null));
            processor.cacheAccessed();
        }
        return result;
    }

    @Override
    public void put(String key, Entry value, StatsProcessor processor) {
        map.put(key, value);
        processor.cacheAccessed();
    }

    @Override
    public void putAll(Map<String, Entry> map, StatsProcessor processor) {
        this.map.putAll(map);
        processor.cacheAccessed();
    }

    @Override
    public void putIfAbsent(String key, Entry value, StatsProcessor processor) {
        map.putIfAbsent(key, value);
        processor.cacheAccessed();
    }

    @Override
    public void putAllIfAbsent(Map<String, Entry> map, StatsProcessor processor) {
        this.map.putAll(map);
        processor.cacheAccessed();
    }

    @Override
    public boolean remove(String key, StatsProcessor processor) {
        map.remove(key);
        processor.cacheAccessed();
        return true;
    }

}
