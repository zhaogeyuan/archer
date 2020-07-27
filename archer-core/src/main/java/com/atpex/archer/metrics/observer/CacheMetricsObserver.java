package com.atpex.archer.metrics.observer;

import com.atpex.archer.metrics.event.CacheEvent;
import com.atpex.archer.metrics.listener.CacheMetricsListener;

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
