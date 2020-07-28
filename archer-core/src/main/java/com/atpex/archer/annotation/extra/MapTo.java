package com.atpex.archer.annotation.extra;

import com.atpex.archer.annotation.CacheMulti;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * {@link CacheMulti} support annotation
 * <p>
 * Map parameter to result element field to </br>
 * make handling noise element or absent element correctly. </br>
 * <p>
 * Noise element example: </br>
 * result: [User(id=1),User(id=2),User(id=3)] </br>
 * parameter: [1,3] </br>
 * noise element in this case is User(id=2) </br>
 * <p>
 * Absent element example: </br>
 * result: [User(id=1),User(id=3)] </br>
 * parameter: [1,2,3] </br>
 * absent element in this case is User(id=2) </br>
 * <p>
 * With this annotation, all parameters could be mapped to absolute result elements.</br>
 *
 * @author atpexgo.wu
 * @see CacheMulti
 * @since 1.0
 */
@Documented
@Retention(RUNTIME)
@Target({PARAMETER})
public @interface MapTo {

    /**
     * Result element class field
     * Spring Expression Language (SpEL) expression is supported
     */
    String value();
}
