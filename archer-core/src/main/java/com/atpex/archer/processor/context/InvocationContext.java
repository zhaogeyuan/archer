package com.atpex.archer.processor.context;

import java.lang.reflect.Method;
import java.util.function.Supplier;

/**
 * @author atpexgo
 * @since 1.0.0
 */
public class InvocationContext<CACHE_CONTEXT> {

    private transient Object target;

    private transient Method method;

    private transient Object[] args;

    private transient Supplier<?> methodInvoker;

    private CACHE_CONTEXT cacheContext;

    public Object getTarget() {
        return target;
    }

    public void setTarget(Object target) {
        this.target = target;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }

    public Supplier<?> getMethodInvoker() {
        return methodInvoker;
    }

    public void setMethodInvoker(Supplier<?> methodInvoker) {
        this.methodInvoker = methodInvoker;
    }

    public CACHE_CONTEXT getCacheContext() {
        return cacheContext;
    }

    public void setCacheContext(CACHE_CONTEXT cacheContext) {
        this.cacheContext = cacheContext;
    }

}
