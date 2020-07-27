package com.atpex.archer.loader;


import com.atpex.archer.processor.context.CacheInvocationContext;
import com.atpex.archer.processor.context.InvocationContext;

import java.util.List;
import java.util.Map;

/**
 * Multi Loader
 *
 * @author atpexgo.wu
 * @since 1.0.0
 */
public interface MultipleLoader<V> extends Loader<InvocationContext<List<CacheInvocationContext<V>>>, Map<CacheInvocationContext<V>, V>> {

}
