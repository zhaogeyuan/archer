package com.atpex.archer.metrics.observer;

import com.himalaya.service.cacheable.interceptor.CacheResolver;
import com.himalaya.service.cacheable.metrics.event.CacheEvent;
import com.himalaya.service.cacheable.metrics.listener.CacheMetricsListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Internal cache metrics observer
 *
 * @param <E>
 * @author atpexgo.wu
 * @since 1.0.0
 */
public class InternalCacheMetricsObserver<E extends CacheEvent> implements CacheMetricsObserver<E> {

    public static final String INTERNAL_CACHE_METRICS_OBSERVER_BEAN_NAME = "service.cacheable.internalCacheMetricsObserver";


    private List<CacheMetricsListener<E>> listeners = new ArrayList<>();

    @Override
    public void register(CacheMetricsListener<E> listener) {
        listeners.add(listener);
    }

    @Override
    public void observe(E event) {
        if (CacheResolver.Config.metricsEnabled) {
            for (CacheMetricsListener<E> listener : listeners) {
                if (listener.filter(event.getClass())) {
                    listener.onEvent(event);
                }
            }
        }
    }
}
