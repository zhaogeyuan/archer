package com.atpex.archer.stats.event.collector;

import com.atpex.archer.stats.event.api.CacheEvent;
import com.atpex.archer.stats.event.api.CacheEventCollector;

/**
 * Named cache event collector
 *
 * @author atpexgo.wu
 * @since 1.0.0
 */
public class NamedCacheEventCollector implements CacheEventCollector {

    private final String name;

    public NamedCacheEventCollector(String name) {
        this.name = name;
    }

    @Override
    public void collect(CacheEvent cacheEvent) {

    }

}
