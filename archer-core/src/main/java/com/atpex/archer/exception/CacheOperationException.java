package com.atpex.archer.exception;

/**
 * Cache operation exception
 *
 * @author atpexgo.wu
 * @since 1.0.0
 */
public class CacheOperationException extends RuntimeException {

    public CacheOperationException() {
    }

    public CacheOperationException(String message) {
        super(message);
    }

    public CacheOperationException(String message, Throwable cause) {
        super(message, cause);
    }

    public CacheOperationException(Throwable cause) {
        super(cause);
    }

    public CacheOperationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
