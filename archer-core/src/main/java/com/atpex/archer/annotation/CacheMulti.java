package com.atpex.archer.annotation;

import com.atpex.archer.components.KeyGenerator;
import com.atpex.archer.components.ValueSerializer;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Cache Multi annotation
 * <p>
 * Use for batch querying method with multiple keys
 *
 * @author atpexgo
 * @since 1.0.0
 */
@Documented
@Retention(RUNTIME)
@Target(METHOD)
public @interface CacheMulti {

    /**
     * Cache key
     * <p>
     * Support Spring Expression Language (SpEL)
     */
    String key();

    /**
     * Expiration
     */
    long expiration() default 3600 * 24 * 7;

    /**
     * Expiration time unit
     */
    TimeUnit expirationTimeUnit() default TimeUnit.SECONDS;

    /**
     * Enable cache breakdown protect
     */
    boolean breakdownProtect() default true;

    /**
     * Breakdown protect timeout
     */
    long breakdownProtectTimeout() default 5;

    /**
     * Breakdown protect timeout time unit
     */
    TimeUnit breakdownProtectTimeUnit() default TimeUnit.SECONDS;

    /**
     * Custom value serializer name
     *
     * @see ValueSerializer
     */
    String valueSerializer() default "";

    /**
     * Custom key generator name
     *
     * @see KeyGenerator
     */
    String keyGenerator() default "";

    /**
     * Cache condition
     * <p>
     * Support Spring Expression Language (SpEL)
     */
    String condition() default "";

    /**
     * Overwrite no matter cache exists or not
     */
    boolean overwrite() default false;

    /**
     * Cache order
     * <p>
     * Support Spring Expression Language (SpEL)
     */
    String orderBy() default "";

}
