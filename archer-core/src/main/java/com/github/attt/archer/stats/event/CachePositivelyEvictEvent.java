package com.github.attt.archer.stats.event;

import com.github.attt.archer.cache.api.Cache;
import com.github.attt.archer.stats.api.CacheEvent;
import com.github.attt.archer.stats.api.CacheEventCollector;

/**
 * Cache positively evict event
 * <p>
 * Produced when {@link Cache#remove(String, CacheEventCollector)} is invoked
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public class CachePositivelyEvictEvent implements CacheEvent {
}
