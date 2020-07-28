package com.atpex.archer.cache.internal;


import com.atpex.archer.cache.api.Cache;
import com.atpex.archer.cache.api.ShardingConfigure;
import com.atpex.archer.stats.event.CacheAccessEvent;
import com.atpex.archer.stats.event.CachePositivelyEvictEvent;
import com.atpex.archer.stats.event.CacheTimeElapsingEvent;
import com.atpex.archer.stats.event.api.CacheEventCollector;
import com.atpex.archer.util.CommonUtils;

import java.util.*;

/**
 * Sharding cache operation source
 *
 * @author atpexgo.wu
 * @since 1.0.0
 */
public class ShardingCache implements Cache {

    private ShardingConfigure shardingConfigure;

    public static final String SHARDING_CACHE_BEAN_NAME = "archer.cache.internalCache";

    public ShardingCache(ShardingConfigure shardingConfigure) {
        this.shardingConfigure = shardingConfigure;
    }

    public void setShardingConfigure(ShardingConfigure shardingConfigure) {
        this.shardingConfigure = shardingConfigure;
    }

    @Override
    public boolean containsKey(String key, CacheEventCollector collector) {
        CacheTimeElapsingEvent cacheTimeElapsingEvent = new CacheTimeElapsingEvent();
        CacheAccessEvent cacheAccessEvent = new CacheAccessEvent();
        boolean exist = shardingConfigure.sharding(key).containsKey(key, collector);
        cacheTimeElapsingEvent.done();
        collector.collect(cacheTimeElapsingEvent);
        collector.collect(cacheAccessEvent);
        return exist;
    }

    @Override
    public Entry get(String key, CacheEventCollector collector) {
        CacheTimeElapsingEvent cacheTimeElapsingEvent = new CacheTimeElapsingEvent();
        Entry entry = shardingConfigure.sharding(key).get(key, collector);
        cacheTimeElapsingEvent.done();
        collector.collect(cacheTimeElapsingEvent);
        return entry;
    }

    @Override
    public Map<String, Entry> getAll(Collection<String> keys, CacheEventCollector collector) {
        CacheTimeElapsingEvent cacheTimeElapsingEvent = new CacheTimeElapsingEvent();
        Map<Cache, List<String>> shardingKeys = new LinkedHashMap<>();
        for (String key : keys) {
            Cache cache = shardingConfigure.sharding(key);
            shardingKeys.computeIfAbsent(cache, cacheKey -> new ArrayList<>()).add(key);
        }

        // make sure list order is right
        Map<String, Entry> keysToResult = new HashMap<>();
        for (Map.Entry<Cache, List<String>> operationSourceKeysEntry : shardingKeys.entrySet()) {
            Cache cache = operationSourceKeysEntry.getKey();
            List<String> keysInOneShard = operationSourceKeysEntry.getValue();
            if (CommonUtils.isEmpty(keysInOneShard)) {
                continue;
            }
            Map<String, Entry> entriesInOneShard = cache.getAll(keysInOneShard, collector);
            keysToResult.putAll(entriesInOneShard);
        }
        cacheTimeElapsingEvent.done();
        collector.collect(cacheTimeElapsingEvent);
        return keysToResult;
    }

    @Override
    public void put(String key, Entry value, CacheEventCollector collector) {
        CacheTimeElapsingEvent cacheTimeElapsingEvent = new CacheTimeElapsingEvent();
        shardingConfigure.sharding(key).put(key, value, collector);
        cacheTimeElapsingEvent.done();
        collector.collect(cacheTimeElapsingEvent);
    }

    @Override
    public void putAll(Map<String, Entry> map, CacheEventCollector collector) {
        doPutAll(map, collector, false);
    }

    @Override
    public void putIfAbsent(String key, Entry value, CacheEventCollector collector) {
        CacheTimeElapsingEvent cacheTimeElapsingEvent = new CacheTimeElapsingEvent();
        shardingConfigure.sharding(key).putIfAbsent(key, value, collector);
        cacheTimeElapsingEvent.done();
        collector.collect(cacheTimeElapsingEvent);
    }

    @Override
    public void putAllIfAbsent(Map<String, Entry> map, CacheEventCollector collector) {
        doPutAll(map, collector, true);
    }

    private void doPutAll(Map<String, Entry> map, CacheEventCollector collector, boolean considerAbsent) {
        CacheTimeElapsingEvent cacheTimeElapsingEvent = new CacheTimeElapsingEvent();
        Map<Cache, Map<String, Entry>> shardingKeys = new HashMap<>();
        for (Map.Entry<String, Entry> kv : map.entrySet()) {
            String key = kv.getKey();
            Entry value = kv.getValue();
            Cache cache = shardingConfigure.sharding(key);
            if (shardingKeys.containsKey(cache)) {
                shardingKeys.get(cache).put(key, value);
            } else {
                Map<String, Entry> keyValuesInOneShard = new HashMap<>();
                keyValuesInOneShard.put(key, value);
                shardingKeys.put(cache, keyValuesInOneShard);
            }
        }

        for (Map.Entry<Cache, Map<String, Entry>> cacheEntry : shardingKeys.entrySet()) {
            Cache cache = cacheEntry.getKey();
            Map<String, Entry> keyValuesInOneShard = cacheEntry.getValue();
            if (considerAbsent) {
                cache.putAllIfAbsent(keyValuesInOneShard, collector);
            } else {
                cache.putAll(keyValuesInOneShard, collector);
            }
        }
        cacheTimeElapsingEvent.done();
        collector.collect(cacheTimeElapsingEvent);
    }

    @Override
    public boolean remove(String key, CacheEventCollector collector) {
        CacheTimeElapsingEvent cacheTimeElapsingEvent = new CacheTimeElapsingEvent();
        CachePositivelyEvictEvent cachePositivelyEvictEvent = new CachePositivelyEvictEvent();
        boolean remove = shardingConfigure.sharding(key).remove(key, collector);
        cacheTimeElapsingEvent.done();
        collector.collect(cacheTimeElapsingEvent);
        collector.collect(cachePositivelyEvictEvent);
        return remove;
    }

    @Override
    public Entry wrap(String key, byte[] value, long ttl) {
        return shardingConfigure.sharding(key).wrap(key, value, ttl);
    }
}
