package com.atpex.archer.cache.api;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

/**
 * Cache operation
 *
 * @author atpexgo.wu
 * @since 1.0.0
 */
public interface Cache {

    default void init(CacheShard shard) {
    }

    boolean containsKey(String key, StatsProcessor processor);

    Entry get(String key, StatsProcessor processor);

    Map<String, Entry> getAll(Collection<String> keys, StatsProcessor processor);

    void put(String key, Entry value, StatsProcessor processor);

    void putAll(Map<String, Entry> map, StatsProcessor processor);

    void putIfAbsent(String key, Entry value, StatsProcessor processor);

    void putAllIfAbsent(Map<String, Entry> map, StatsProcessor processor);

    boolean remove(String key, StatsProcessor processor);

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

    interface StatsProcessor {

        void cacheAccessed();
    }
}
