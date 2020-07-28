package com.atpex.archer.stats.api;

import com.atpex.archer.stats.api.listener.CacheStatsListener;


/**
 * Cache event collector
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public interface CacheEventCollector {

    void register(CacheStatsListener<CacheEvent> listener);

    void collect(CacheEvent cacheEvent);
}
