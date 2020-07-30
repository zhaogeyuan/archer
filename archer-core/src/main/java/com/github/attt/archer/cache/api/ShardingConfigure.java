package com.github.attt.archer.cache.api;

/**
 * @author atpexgo.wu
 * @since 1.0
 */
public interface ShardingConfigure {
    Cache sharding(String seed);
}
