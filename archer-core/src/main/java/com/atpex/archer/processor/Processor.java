package com.atpex.archer.processor;


import com.atpex.archer.processor.context.CacheInvocationContext;
import com.atpex.archer.processor.context.InvocationContext;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Cache processor
 *
 * @param <V> value type
 * @author atpexgo
 * @since 1.0.0
 */
public interface Processor<V> {

    V get(InvocationContext<CacheInvocationContext<V>> invocationContext);

    Map<CacheInvocationContext<V>, V> getAll(InvocationContext<List<CacheInvocationContext<V>>> invocationContext);

    void put(InvocationContext<CacheInvocationContext<V>> invocationContext, V value);

    void putAll(InvocationContext<Map<CacheInvocationContext<V>, V>> invocationContext);

    void delete(InvocationContext<CacheInvocationContext<V>> invocationContext);

    class BreakdownProtectionLock<V> {
        CountDownLatch signal;
        Thread loaderThread;
        volatile boolean success;
        volatile V value;
    }
}
