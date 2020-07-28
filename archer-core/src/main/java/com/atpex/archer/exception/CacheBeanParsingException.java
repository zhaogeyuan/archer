package com.atpex.archer.exception;

/**
 * Cache bean parsing exception
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public class CacheBeanParsingException extends RuntimeException {

    public CacheBeanParsingException() {
    }

    public CacheBeanParsingException(String message) {
        super(message);
    }

    public CacheBeanParsingException(String message, Throwable cause) {
        super(message, cause);
    }

    public CacheBeanParsingException(Throwable cause) {
        super(cause);
    }

    public CacheBeanParsingException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
