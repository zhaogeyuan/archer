package com.atpex.archer.loader;


import com.atpex.archer.processor.context.CacheInvocationContext;
import com.atpex.archer.processor.context.InvocationContext;

/**
 * Loader
 *
 * @author atpexgo.wu
 * @since 1.0.0
 */
public interface SingleLoader<V> extends Loader<InvocationContext<CacheInvocationContext<V>>, V> {

}
