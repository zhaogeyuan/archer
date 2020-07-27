package com.atpex.archer;

/**
 * Cache component
 * <p>
 * As a component of service cache
 *
 * @author atpexgo.wu
 * @since 1.0.0
 */
public interface Component {

    default void initialized() {
    }

    default String initializedInfo() {
        return "";
    }

}
