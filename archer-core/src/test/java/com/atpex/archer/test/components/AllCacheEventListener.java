package com.atpex.archer.test.components;

import com.atpex.archer.stats.api.CacheEvent;
import com.atpex.archer.stats.api.listener.CacheStatsListener;
import com.atpex.archer.stats.event.CacheTimeElapsingEvent;

import java.util.function.Predicate;

/**
 * @author: atpex
 * @since 1.0
 */
public class AllCacheEventListener implements CacheStatsListener<CacheEvent> {

    @Override
    public void onEvent(String name, CacheEvent event) {
        if (event instanceof CacheTimeElapsingEvent) {
            System.out.println(name + " : " + event.getClass().getSimpleName() + " : " + ((CacheTimeElapsingEvent) event).elapsing() + "ns");
        } else {
            System.out.println(name + " : " + event.getClass().getSimpleName());
        }
    }

    @Override
    public Predicate<CacheEvent> filter() {
        return (e) -> true;
    }

    @Override
    public int compareTo(CacheStatsListener<CacheEvent> o) {
        return 0;
    }
}
