package com.atpex.archer.stats.listener;

import com.atpex.archer.stats.event.api.CacheEvent;

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
