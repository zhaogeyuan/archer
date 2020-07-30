package com.github.attt.archer.roots;

/**
 * Cache component
 * <p>
 * As a component of framework
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public interface Component {

    default void initialized() {
    }

    default String initializedInfo() {
        return "";
    }

}
