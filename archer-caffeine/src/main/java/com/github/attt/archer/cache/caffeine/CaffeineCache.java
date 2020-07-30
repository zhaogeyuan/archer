package com.github.attt.archer.cache.caffeine;

import com.github.attt.archer.cache.api.CacheShard;
import com.github.attt.archer.cache.preset.HashMapCache;
import com.github.attt.archer.stats.api.CacheEventCollector;
import com.github.attt.archer.stats.event.CacheAccessEvent;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Caffeine cache operation
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public final class CaffeineCache extends HashMapCache {

    private Cache<String, Entry> cache;


    @Override
    public void init(CacheShard shard) {
        if (!(shard instanceof CaffeineConfig)) {
            throw new RuntimeException("Cache operation shard info supplied is not a instance of CaffeineShard");
        }
        cache = Caffeine.newBuilder()
                .initialCapacity(((CaffeineConfig) shard).getInitialCapacity())
                .maximumSize(((CaffeineConfig) shard).getMaximumSize())
                .expireAfterWrite(((CaffeineConfig) shard).getExpireAfterWriteInSecond(), TimeUnit.SECONDS)
                .build();
    }

    @Override
    public boolean containsKey(String key, CacheEventCollector collector) {
        boolean exists = cache.getIfPresent(key) != null;
        collector.collect(new CacheAccessEvent());
        return exists;
    }

    @Override
    public Map<String, Entry> getAll(Collection<String> keys, CacheEventCollector collector) {
        Map<String, Entry> allPresent = cache.getAllPresent(keys);
        collector.collect(new CacheAccessEvent());
        for (String key : keys) {
            if (!allPresent.containsKey(key)) {
                allPresent.put(key, null);
            }
        }
        return allPresent;
    }

    @Override
    public void put(String key, Entry value, CacheEventCollector collector) {
        cache.put(key, value);
        collector.collect(new CacheAccessEvent());
    }

    @Override
    public void putAll(Map<String, Entry> map, CacheEventCollector collector) {
        cache.putAll(map);
        collector.collect(new CacheAccessEvent());
    }

    @Override
    public void putIfAbsent(String key, Entry value, CacheEventCollector collector) {
        cache.get(key, s -> {
            Entry present = cache.getIfPresent(s);
            collector.collect(new CacheAccessEvent());
            if (present == null) {
                cache.put(key, value);
                collector.collect(new CacheAccessEvent());
                return value;
            }
            return present;
        });
    }

    @Override
    public boolean remove(String key, CacheEventCollector collector) {
        cache.invalidate(key);
        collector.collect(new CacheAccessEvent());
        return false;
    }

    @Override
    public boolean removeAll(Collection<String> keys, CacheEventCollector collector) {
        cache.invalidateAll(keys);
        collector.collect(new CacheAccessEvent());
        return false;
    }

    @Override
    public void putAllIfAbsent(Map<String, Entry> map, CacheEventCollector collector) {
        map.forEach((k, e) -> putIfAbsent(k, e, collector));
    }

    @Override
    public Entry get(String key, CacheEventCollector collector) {
        Entry entry = cache.getIfPresent(key);
        collector.collect(new CacheAccessEvent());
        return entry;
    }
}
