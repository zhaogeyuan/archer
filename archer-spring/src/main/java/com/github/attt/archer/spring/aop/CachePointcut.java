package com.github.attt.archer.spring.aop;


import com.github.attt.archer.util.ReflectionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.ClassFilter;
import org.springframework.aop.support.StaticMethodMatcherPointcut;

import java.lang.reflect.Method;

/**
 * Cache point cut
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public class CachePointcut extends StaticMethodMatcherPointcut implements ClassFilter {

    private static final Logger logger = LoggerFactory.getLogger(CachePointcut.class);

    private String[] basePackages;

    public CachePointcut(String[] basePackages) {
        setClassFilter(this);
        this.basePackages = basePackages;
    }

    @Override
    public boolean matches(Class clazz) {
        boolean b = matchesImpl(clazz);
        logger.trace("check class match {}: {}", b, clazz);
        return b;
    }

    private boolean matchesImpl(Class clazz) {
        if (matchesThis(clazz)) {
            return true;
        }
        Class[] cs = clazz.getInterfaces();
        if (cs != null) {
            for (Class c : cs) {
                if (matchesImpl(c)) {
                    return true;
                }
            }
        }
        if (!clazz.isInterface()) {
            Class sp = clazz.getSuperclass();
            if (sp != null && matchesImpl(sp)) {
                return true;
            }
        }
        return false;
    }

    public boolean matchesThis(Class clazz) {
        String name = clazz.getName();
        if (exclude(name)) {
            return false;
        }
        return include(name);
    }

    private boolean include(String name) {
        if (basePackages != null) {
            for (String p : basePackages) {
                if (name.startsWith(p)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean exclude(String name) {
        if (name.startsWith("java")) {
            return true;
        }
        if (name.startsWith("org.springframework")) {
            return true;
        }
        if (name.indexOf("$$EnhancerBySpringCGLIB$$") >= 0) {
            return true;
        }
        if (name.indexOf("$$FastClassBySpringCGLIB$$") >= 0) {
            return true;
        }
        return false;
    }

    @Override
    public boolean matches(Method method, Class targetClass) {
        boolean b = matchesImpl(method, targetClass);
        if (b) {
            logger.debug("check method match true: method={}, declaringClass={}, targetClass={}",
                    method.getName(),
                    method.getDeclaringClass().getName(),
                    targetClass == null ? null : targetClass.getName());
        } else {
            logger.trace("check method match false: method={}, declaringClass={}, targetClass={}",
                    method.getName(),
                    method.getDeclaringClass().getName(),
                    targetClass == null ? null : targetClass.getName());
        }
        return b;
    }

    private boolean matchesImpl(Method method, Class targetClass) {
        if (!matchesThis(method.getDeclaringClass())) {
            return false;
        }
        if (exclude(targetClass.getName())) {
            return false;
        }
        boolean cacheAnnotationExists = cacheAnnotationExists(method);
        if (!cacheAnnotationExists) {
            try {
                Method targetClassDeclaredMethod = targetClass.getMethod(method.getName(), method.getParameterTypes());
                cacheAnnotationExists = cacheAnnotationExists(targetClassDeclaredMethod);
            } catch (NoSuchMethodException ignored) {
            }
        }
        return cacheAnnotationExists;
    }

    private boolean cacheAnnotationExists(Method method) {
        return ReflectionUtil.findAnyCacheAnnotation(method);
    }

}
