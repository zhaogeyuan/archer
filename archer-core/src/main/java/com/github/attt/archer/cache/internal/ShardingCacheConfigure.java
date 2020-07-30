package com.github.attt.archer.cache.internal;

import com.github.attt.archer.cache.api.Cache;
import com.github.attt.archer.cache.api.CacheInitializer;
import com.github.attt.archer.cache.api.CacheShard;
import com.github.attt.archer.cache.api.ShardingConfigure;
import com.github.attt.archer.cache.preset.HashMapCache;
import com.github.attt.archer.exception.CacheBeanParsingException;
import com.github.attt.archer.util.CommonUtils;
import com.github.attt.archer.util.ShardingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author atpexgo.wu
 * @since 1.0
 */
public class ShardingCacheConfigure implements ShardingConfigure {

    private static final Logger logger = LoggerFactory.getLogger(ShardingCacheConfigure.class);

    private static final int NODE_NUM = 50;

    protected static volatile Boolean initialized = false;

    protected List<CacheShard> shardList;

    private CacheInitializer cacheInitializer;

    private TreeMap<Long, Cache> shardingNodes;

    private HashMapCache hashMapCache = new HashMapCache();

    private boolean operationProvided = false;

    public ShardingCacheConfigure(CacheInitializer initializer, List<CacheShard> shardList) {
        this.cacheInitializer = initializer;
        this.shardList = shardList;
    }

    @Override
    public Cache sharding(String seed) {
        if (!operationProvided) {
            return hashMapCache;
        }
        SortedMap<Long, Cache> tail = shardingNodes.tailMap(ShardingUtil.hash(seed));
        if (tail.size() == 0) {
            return shardingNodes.get(shardingNodes.firstKey());
        }
        return tail.get(tail.firstKey());
    }

    public void init() {
        if (!CommonUtils.isEmpty(this.shardList)) {
            operationProvided = true;
            shardingNodes = new TreeMap<>();

            initialized = true;
            if (cacheInitializer != null) {
                initialCache();
            }
        }
    }

    private void initialCache() {
        logger.debug("Initialize cache operation, type : {}", cacheInitializer);

        for (int i = 0; i < this.shardList.size(); i++) {
            CacheShard shard = shardList.get(i);
            try {
                Cache cache = cacheInitializer.initial(shard);
                for (int j = 0; j < NODE_NUM; j++) {
                    shardingNodes.put(ShardingUtil.hash("SHARD-" + i + "NODE-" + j), cache);
                }
            } catch (Throwable t) {
                throw new CacheBeanParsingException("Create cache operation failed", t);
            }
        }
    }
}
