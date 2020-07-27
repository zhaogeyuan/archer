package com.atpex.archer.metrics.listener;

import com.atpex.archer.metrics.event.CacheEvent;
import com.atpex.archer.metrics.event.CacheInvokedEvent;

/**
 * Cache invoked listener
 *
 * @author atpexgo.wu
 * @since 1.0.0
 */
public interface CacheInvokedListener extends CacheMetricsListener<CacheInvokedEvent> {

    @Override
    void onEvent(CacheInvokedEvent event);

    @Override
    default boolean filter(Class<? extends CacheEvent> eventClass) {
        return eventClass == CacheInvokedEvent.class;
    }

    @Override
    default int compareTo(CacheMetricsListener<CacheInvokedEvent> o) {
        return 0;
    }
}
