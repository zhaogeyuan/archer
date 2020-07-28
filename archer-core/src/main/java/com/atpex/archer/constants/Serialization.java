package com.atpex.archer.constants;

import com.atpex.archer.components.preset.FastJsonObjectSerializer;
import com.atpex.archer.components.preset.HessianObjectSerializer;
import com.atpex.archer.components.preset.JavaObjectSerializer;
import com.atpex.archer.components.preset.KryoObjectSerializer;

/**
 * @author atpexgo.wu
 * @since 1.0
 */
public enum Serialization {

    /**
     * Alibaba fastjson
     *
     * @see FastJsonObjectSerializer
     */
    FAST_JSON("com.alibaba.fastjson.JSON"),

    /**
     * Kryo
     *
     * @see KryoObjectSerializer
     */
    KRYO("com.esotericsoftware.kryo.Kryo"),

    /**
     * Hessian
     *
     * @see HessianObjectSerializer
     */
    HESSIAN("com.caucho.hessian.io.HessianInput"),

    /**
     * Java
     *
     * @see JavaObjectSerializer
     */
    JAVA("java.lang.Object");

    private final String mainDependency;

    public String getMainDependency() {
        return mainDependency;
    }

    Serialization(String mainDependency) {
        this.mainDependency = mainDependency;
    }
}
