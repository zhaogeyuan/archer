package com.atpex.archer.metrics.listener;

import com.atpex.archer.metrics.event.CacheEvent;
import com.atpex.archer.metrics.event.CacheHitRateEvent;

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
