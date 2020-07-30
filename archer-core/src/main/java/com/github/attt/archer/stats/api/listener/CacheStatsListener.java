package com.github.attt.archer.stats.api.listener;

import com.github.attt.archer.stats.api.CacheEvent;

import java.util.EventListener;
import java.util.function.Predicate;

/**
 * Cache stats listener
 *
 * @param <E> cache event
 * @author atpexgo.wu
 * @since 1.0
 */
public interface CacheStatsListener<E extends CacheEvent> extends Comparable<CacheStatsListener<E>>, EventListener {

    void onEvent(String name, E event);

    Predicate<E> filter();
}
