package com.github.attt.archer.cache.api;

import com.github.attt.archer.roots.Component;

/**
 * Cache operation initialization processor
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public interface CacheInitializer extends Component {


    Cache initial(CacheShard shard) throws Throwable;

    default boolean enabled() {
        return true;
    }

    default int order() {
        return -1;
    }
}
