package com.atpex.archer.metrics.listener;

import com.atpex.archer.metrics.event.CacheEvent;

import java.util.EventListener;

/**
 * Cache metrics listener
 *
 * @param <E>
 * @author atpexgo.wu
 * @since 1.0.0
 */
public interface CacheMetricsListener<E extends CacheEvent> extends Comparable<CacheMetricsListener<E>>, EventListener {

    void onEvent(E event);

    boolean filter(Class<? extends CacheEvent> eventClass);
}
