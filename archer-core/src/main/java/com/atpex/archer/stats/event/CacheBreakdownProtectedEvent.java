package com.atpex.archer.stats.event;

import com.atpex.archer.annotation.Cache;
import com.atpex.archer.processor.api.AbstractProcessor;
import com.atpex.archer.stats.api.CacheEvent;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Cache breakdown protected event
 * <p>
 * Produced when any request that could causing cache breakdown,</br>
 * if breakdown protect option (such as {@link Cache#breakdownProtect()}) is set to true
 *
 * @author atpexgo.wu
 * @see AbstractProcessor.BreakdownProtectionLock
 * @see AbstractProcessor#doSynchronizedLoadAndPut(String, long, Supplier, Consumer, Supplier)
 * @since 1.0
 */
public class CacheBreakdownProtectedEvent implements CacheEvent {
}
