package com.github.attt.archer.annotation.extra;

import com.github.attt.archer.annotation.CacheList;
import com.github.attt.archer.annotation.CacheMulti;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Support {@link java.util.Map} result for {@link CacheMulti} and {@link CacheList}
 * <p>
 *
 * @author atpexgo
 * @see CacheMulti
 * @see CacheList
 * @since 1.0
 */
@Documented
@Retention(RUNTIME)
@Target({METHOD})
public @interface HashKey {

    /**
     * Support Spring Expression Language (SpEL)
     */
    String value();
}
