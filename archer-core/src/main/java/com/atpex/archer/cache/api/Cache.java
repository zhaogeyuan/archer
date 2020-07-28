package com.atpex.archer.cache.api;

import com.atpex.archer.stats.api.CacheEventCollector;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

/**
 * Cache operation
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public interface Cache {

    default void init(CacheShard shard) {
    }

    boolean containsKey(String key, CacheEventCollector collector);

    Entry get(String key, CacheEventCollector collector);

    Map<String, Entry> getAll(Collection<String> keys, CacheEventCollector collector);

    void put(String key, Entry value, CacheEventCollector collector);

    void putAll(Map<String, Entry> map, CacheEventCollector collector);

    void putIfAbsent(String key, Entry value, CacheEventCollector collector);

    void putAllIfAbsent(Map<String, Entry> map, CacheEventCollector collector);

    boolean remove(String key, CacheEventCollector collector);

    default Entry wrap(String key, byte[] value, long ttl) {
        return new DefaultEntry(key, value, ttl);
    }

    interface Entry {

        String getKey();

        byte[] getValue();

        long getTtl();
    }

    class DefaultEntry implements Entry, Serializable {

        private String key;

        private byte[] value;

        private long ttl;

        public DefaultEntry(String key, byte[] value, long ttl) {
            this.key = key;
            this.value = value;
            this.ttl = ttl;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public void setValue(byte[] value) {
            this.value = value;
        }

        public void setTtl(long ttl) {
            this.ttl = ttl;
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public byte[] getValue() {
            return value;
        }

        @Override
        public long getTtl() {
            return ttl;
        }
    }
}
