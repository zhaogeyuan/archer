package com.atpex.archer.stats.event;

import com.atpex.archer.stats.api.CacheEvent;
import com.atpex.archer.stats.api.CacheEventCollector;
import com.atpex.archer.cache.api.Cache;

/**
 * Cache positively evict event
 *
 * Produced when {@link Cache#remove(String, CacheEventCollector)} is invoked
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public class CachePositivelyEvictEvent implements CacheEvent {
}
