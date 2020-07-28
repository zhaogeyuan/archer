package com.atpex.archer.stats.event.api;

/**
 * Cache event collector
 *
 * @author atpexgo.wu
 * @since 1.0.0
 */
public interface CacheEventCollector {

    void collect(CacheEvent cacheEvent);
}
