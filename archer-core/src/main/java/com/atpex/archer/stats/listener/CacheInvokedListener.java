package com.atpex.archer.stats.listener;

import com.atpex.archer.stats.event.api.CacheEvent;

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
