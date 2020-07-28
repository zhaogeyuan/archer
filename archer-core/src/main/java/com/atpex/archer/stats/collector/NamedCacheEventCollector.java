package com.atpex.archer.stats.collector;

import com.atpex.archer.stats.api.CacheEvent;
import com.atpex.archer.stats.api.CacheEventCollector;
import com.atpex.archer.stats.api.listener.CacheStatsListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Named cache event collector
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public class NamedCacheEventCollector implements CacheEventCollector {

    private String name;

    private List<CacheStatsListener<CacheEvent>> listeners = new ArrayList<>();

    public NamedCacheEventCollector(String name) {
        this.name = name;
    }


    @Override
    public void register(CacheStatsListener<CacheEvent> listener) {
        listeners.add(listener);
    }

    @Override
    public void collect(CacheEvent cacheEvent) {
        for (CacheStatsListener<CacheEvent> listener : listeners) {
            if (listener.filter().test(cacheEvent)) {
                listener.onEvent(name, cacheEvent);
            }
        }
    }
}
