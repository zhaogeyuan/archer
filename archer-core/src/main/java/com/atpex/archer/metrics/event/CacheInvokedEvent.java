package com.atpex.archer.metrics.event;

/**
 * Cache invoke event
 *
 * @author atpexgo.wu
 * @since 1.0.0
 */
public class CacheInvokedEvent implements CacheEvent {

    private boolean invoked;

    public CacheInvokedEvent() {
    }

    public boolean isInvoked() {
        return invoked;
    }

    public void setInvoked(boolean invoked) {
        this.invoked = invoked;
    }

    @Override
    public String toString() {
        return "CacheInvokedEvent{" +
                "invoked=" + invoked +
                '}';
    }
}
