package com.atpex.archer.cache.internal;


import com.atpex.archer.cache.api.Cache;
import com.atpex.archer.cache.api.ShardingConfigure;
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
    public boolean containsKey(String key, StatsProcessor processor) {
        return shardingConfigure.sharding(key).containsKey(key, processor);
    }

    @Override
    public Entry get(String key, StatsProcessor processor) {
        return shardingConfigure.sharding(key).get(key, processor);
    }

    @Override
    public Map<String, Entry> getAll(Collection<String> keys, StatsProcessor processor) {
        Map<Cache, List<String>> shardingKeys = new LinkedHashMap<>();
        for (String key : keys) {
            Cache cache = shardingConfigure.sharding(key);
            shardingKeys.computeIfAbsent(cache, cacheKey -> new ArrayList<>()).add(key);
        }

        // make sure list order is right
        Map<String, Entry> keysToResult = new HashMap<>();
        for (Map.Entry<Cache, List<String>> operationSourceKeysEntry : shardingKeys.entrySet()) {
            Cache shardOperationSource = operationSourceKeysEntry.getKey();
            List<String> keysInOneShard = operationSourceKeysEntry.getValue();
            if (CommonUtils.isEmpty(keysInOneShard)) {
                continue;
            }
            Map<String, Entry> entriesInOneShard = shardOperationSource.getAll(keysInOneShard, processor);
            keysToResult.putAll(entriesInOneShard);
        }

        return keysToResult;
    }

    @Override
    public void put(String key, Entry value, StatsProcessor processor) {
        shardingConfigure.sharding(key).put(key, value, processor);
    }

    @Override
    public void putAll(Map<String, Entry> map, StatsProcessor processor) {
        doPutAll(map, processor, false);
    }

    @Override
    public void putIfAbsent(String key, Entry value, StatsProcessor processor) {
        shardingConfigure.sharding(key).putIfAbsent(key, value, processor);
    }

    @Override
    public void putAllIfAbsent(Map<String, Entry> map, StatsProcessor processor) {
        doPutAll(map, processor, true);
    }

    private void doPutAll(Map<String, Entry> map, StatsProcessor processor, boolean considerAbsent) {
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
                cache.putAllIfAbsent(keyValuesInOneShard, processor);
            } else {
                cache.putAll(keyValuesInOneShard, processor);
            }
        }
    }

    @Override
    public boolean remove(String key, StatsProcessor processor) {
        return shardingConfigure.sharding(key).remove(key, processor);
    }

    @Override
    public Entry wrap(String key, byte[] value, long ttl) {
        return shardingConfigure.sharding(key).wrap(key, value, ttl);
    }

    public Cache shard(String key) {
        return shardingConfigure.sharding(key);
    }
}
