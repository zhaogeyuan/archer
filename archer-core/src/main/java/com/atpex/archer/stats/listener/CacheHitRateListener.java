package com.atpex.archer.stats.listener;

import com.atpex.archer.stats.event.api.CacheEvent;

/**
 * Cache hit rate listener
 *
 * @author atpexgo.wu
 * @since 1.0.0
 */
public interface CacheHitRateListener extends CacheMetricsListener<CacheHitRateEvent> {

    @Override
    void onEvent(CacheHitRateEvent event);

    @Override
    default boolean filter(Class<? extends CacheEvent> eventClass) {
        return eventClass == CacheHitRateEvent.class;
    }

    @Override
    default int compareTo(CacheMetricsListener<CacheHitRateEvent> o) {
        return 0;
    }
}
