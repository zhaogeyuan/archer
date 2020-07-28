package com.atpex.archer.cache.preset;

import com.atpex.archer.cache.api.Cache;
import com.atpex.archer.stats.api.CacheEventCollector;
import com.atpex.archer.stats.event.CacheAccessEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Hash map operation cache operation
 * Only for debugging
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public class HashMapCache implements Cache {

    private static final Logger logger = LoggerFactory.getLogger(HashMapCache.class);

    private final Map<String, Entry> unsafe = new WeakHashMap<>();

    private final Map<String, Entry> map = Collections.synchronizedMap(unsafe);

    @Override
    public boolean containsKey(String key, CacheEventCollector collector) {
        boolean exists = map.containsKey(key);
        collector.collect(new CacheAccessEvent());
        return exists;
    }

    @Override
    public Entry get(String key, CacheEventCollector collector) {
        Entry entry = map.get(key);
        collector.collect(new CacheAccessEvent());
        return entry;
    }

    @Override
    public Map<String, Entry> getAll(Collection<String> keys, CacheEventCollector collector) {
        if (keys == null) {
            return new HashMap<>(0);
        }
        Map<String, Entry> result = new HashMap<>(keys.size());
        for (String key : keys) {
            result.put(key, map.getOrDefault(key, null));
            collector.collect(new CacheAccessEvent());
        }
        return result;
    }

    @Override
    public void put(String key, Entry value, CacheEventCollector collector) {
        map.put(key, value);
        collector.collect(new CacheAccessEvent());
    }

    @Override
    public void putAll(Map<String, Entry> map, CacheEventCollector collector) {
        this.map.putAll(map);
        collector.collect(new CacheAccessEvent());
    }

    @Override
    public void putIfAbsent(String key, Entry value, CacheEventCollector collector) {
        map.putIfAbsent(key, value);
        collector.collect(new CacheAccessEvent());
    }

    @Override
    public void putAllIfAbsent(Map<String, Entry> map, CacheEventCollector collector) {
        this.map.putAll(map);
        collector.collect(new CacheAccessEvent());
    }

    @Override
    public boolean remove(String key, CacheEventCollector collector) {
        map.remove(key);
        collector.collect(new CacheAccessEvent());
        return true;
    }

}
