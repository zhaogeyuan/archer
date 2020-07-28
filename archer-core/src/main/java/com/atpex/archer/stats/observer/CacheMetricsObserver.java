package com.atpex.archer.stats.observer;

import com.atpex.archer.stats.event.api.CacheEvent;
import com.atpex.archer.stats.listener.CacheMetricsListener;

/**
 * Cache metrics observer
 *
 * @param <E>
 * @author atpexgo.wu
 * @since 1.0.0
 */
public interface CacheMetricsObserver<E extends CacheEvent> {

    void register(CacheMetricsListener<E> listener);

    void observe(E event);
}
