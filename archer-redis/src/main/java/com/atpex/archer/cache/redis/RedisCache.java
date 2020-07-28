package com.atpex.archer.cache.redis;

import com.atpex.archer.cache.api.Cache;
import com.atpex.archer.cache.api.CacheShard;
import com.atpex.archer.components.api.Serializer;
import com.atpex.archer.components.api.ValueSerializer;
import com.atpex.archer.exception.CacheBeanParsingException;
import com.atpex.archer.stats.api.CacheEventCollector;
import com.atpex.archer.stats.event.CacheAccessEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Pipeline;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Redis cache operation
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public final class RedisCache implements Cache {

    private static final Logger logger = LoggerFactory.getLogger(RedisCache.class);

    private JedisPool jedisPool;

    private Serializer<String, byte[]> keySerializer;

    @SuppressWarnings("all")
    private ValueSerializer valueSerializer;

    public void setKeySerializer(Serializer<String, byte[]> keySerializer) {
        this.keySerializer = keySerializer;
    }

    public void setValueSerializer(@SuppressWarnings("all") ValueSerializer valueSerializer) {
        this.valueSerializer = valueSerializer;
    }

    @Override
    public void init(CacheShard shard) {
        if (!(shard instanceof RedisShard)) {
            throw new CacheBeanParsingException("Cache operation shard info supplied is not a instance of RedisShard");
        }
        RedisShard redisShard = (RedisShard) shard;

        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setLifo(redisShard.isLifo());
        jedisPoolConfig.setFairness(redisShard.isFairness());
        jedisPoolConfig.setMaxTotal(redisShard.getMaxTotal());
        jedisPoolConfig.setMaxIdle(redisShard.getMaxIdle());
        jedisPoolConfig.setMinIdle(redisShard.getMinIdle());
        jedisPoolConfig.setMaxWaitMillis(redisShard.getMaxWaitMillis());
        jedisPoolConfig.setMinEvictableIdleTimeMillis(redisShard.getMinEvictableIdleTimeMillis());
        jedisPoolConfig.setSoftMinEvictableIdleTimeMillis(redisShard.getSoftMinEvictableIdleTimeMillis());
        jedisPoolConfig.setNumTestsPerEvictionRun(redisShard.getNumTestsPerEvictionRun());
        jedisPoolConfig.setTestOnCreate(redisShard.isTestOnCreate());
        jedisPoolConfig.setTestOnBorrow(redisShard.isTestOnBorrow());
        jedisPoolConfig.setTestOnReturn(redisShard.isTestOnReturn());
        jedisPoolConfig.setTestWhileIdle(redisShard.isTestWhileIdle());
        jedisPoolConfig.setTimeBetweenEvictionRunsMillis(redisShard.getTimeBetweenEvictionRunsMillis());
        jedisPoolConfig.setBlockWhenExhausted(redisShard.isBlockWhenExhausted());

        if (redisShard.getPassword() == null || "".equals(redisShard.getPassword())) {
            jedisPool = new JedisPool(jedisPoolConfig, redisShard.getHost(), redisShard.getPort(), (int) redisShard.getConnectTimeout(), null, redisShard.getDatabase(), redisShard.isSsl());
        } else {
            jedisPool = new JedisPool(jedisPoolConfig, redisShard.getHost(), redisShard.getPort(), (int) redisShard.getConnectTimeout(), redisShard.getPassword(), redisShard.getDatabase(), redisShard.isSsl());
        }
    }

    @Override
    public boolean containsKey(String key, CacheEventCollector collector) {
        Boolean exists = autoClose(jedis -> {
            Boolean hasKey = jedis.exists(key);
            collector.collect(new CacheAccessEvent());
            return hasKey != null && hasKey;
        });
        return exists;
    }

    private Jedis jedis() {
        return jedisPool.getResource();
    }

    @Override
    public Entry get(String key, CacheEventCollector collector) {
        Entry entry = autoClose(jedis -> {
            byte[] value = jedis.get(keySerializer.serialize(key));
            collector.collect(new CacheAccessEvent());
            if (value == null) {
                return null;
            }
            return (Entry) valueSerializer.looseDeserialize(value);
        });

        return entry;
    }

    @Override
    public Map<String, Entry> getAll(Collection<String> keys, CacheEventCollector collector) {
        Map<String, Entry> map = autoClose(jedis -> {
            Map<String, Entry> entryMap = new HashMap<>();
            List<byte[]> values = jedis.mget(keys.stream().map(k -> keySerializer.serialize(k)).toArray(byte[][]::new));
            collector.collect(new CacheAccessEvent());
            String[] keysArray = keys.toArray(new String[0]);
            for (int i = 0; i < keysArray.length; i++) {
                byte[] valueBytes = values.get(i);
                if (valueBytes == null) {
                    entryMap.put(keysArray[i], null);
                } else {
                    entryMap.put(keysArray[i], (Entry) valueSerializer.looseDeserialize(valueBytes));
                }
            }
            return entryMap;
        });
        return map;
    }

    @Override
    public void put(String key, Entry value, CacheEventCollector collector) {
        execute(putOp(key, value));
        collector.collect(new CacheAccessEvent());
    }

    @Override
    public void putAll(Map<String, Entry> map, CacheEventCollector collector) {
        execute(putAllOp(map));
        collector.collect(new CacheAccessEvent());
    }

    @Override
    public void putIfAbsent(String key, Entry value, CacheEventCollector collector) {
        execute(putIfAbsentOp(key, value));
        collector.collect(new CacheAccessEvent());
    }

    @Override
    public void putAllIfAbsent(Map<String, Entry> map, CacheEventCollector collector) {
        execute(putAllIfAbsentOp(map));
        collector.collect(new CacheAccessEvent());
    }

    @Override
    public boolean remove(String key, CacheEventCollector collector) {
        return autoClose(jedis -> jedis.del(keySerializer.serialize(key)) != 0);
    }

    private <R> R autoClose(Function<Jedis, R> function) {
        try (Jedis jedis = jedis()) {
            return function.apply(jedis);
        }
    }

    private void execute(Consumer<Pipeline> redisCallback) {
        autoClose(jedis -> {
            Pipeline pipelined = jedis.pipelined();
            redisCallback.accept(pipelined);
            pipelined.sync();
            return null;
        });
    }

    @SuppressWarnings("all")
    private Consumer<Pipeline> putOp(String key, Entry value) {
        return pipeline -> {
            pipeline.set(keySerializer.serialize(key), (byte[]) valueSerializer.serialize(value));
            if (value.getTtl() != -1L) {
                pipeline.pexpire(keySerializer.serialize(key), value.getTtl());
            }
        };
    }

    @SuppressWarnings("all")
    private Consumer<Pipeline> putAllOp(Map<String, Entry> stringMap) {
        return pipeline -> {
            for (Map.Entry<String, Entry> kv : stringMap.entrySet()) {
                pipeline.set(keySerializer.serialize(kv.getKey()), (byte[]) valueSerializer.serialize(kv.getValue()));
                if (kv.getValue().getTtl() != -1L) {
                    pipeline.pexpire(keySerializer.serialize(kv.getKey()), kv.getValue().getTtl());
                }
            }
        };
    }

    @SuppressWarnings("all")
    private Consumer<Pipeline> putAllIfAbsentOp(Map<String, Entry> stringMap) {
        return pipeline -> {
            for (Map.Entry<String, Entry> kv : stringMap.entrySet()) {
                pipeline.setnx(keySerializer.serialize(kv.getKey()), (byte[]) valueSerializer.serialize(kv.getValue()));
                if (kv.getValue().getTtl() != -1L) {
                    pipeline.pexpire(keySerializer.serialize(kv.getKey()), kv.getValue().getTtl());
                }
            }
        };
    }


    @SuppressWarnings("all")
    private Consumer<Pipeline> putIfAbsentOp(String key, Entry value) {
        return pipeline -> {
            pipeline.setnx(keySerializer.serialize(key), (byte[]) valueSerializer.serialize(value));
            if (value.getTtl() != -1L) {
                pipeline.pexpire(keySerializer.serialize(key), value.getTtl());
            }
        };
    }
}
