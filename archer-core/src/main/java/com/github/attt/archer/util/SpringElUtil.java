package com.github.attt.archer.util;

import com.github.attt.archer.exception.CacheOperationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.ReflectiveMethodResolver;
import org.springframework.expression.spel.support.ReflectivePropertyAccessor;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.Function;

/**
 * SpringEL expression util
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public class SpringElUtil {

    private static final Logger log = LoggerFactory.getLogger(SpringElUtil.class);

    private static Function<Method, String[]> parameterNameDiscoverer;
    private static ExpressionParser parser = new SpelExpressionParser();
    private static ReflectivePropertyAccessor privatePropertyAccessor = new CuFeRr();
    private static ReflectiveMethodResolver privateMethodResolver = new CuMeRr();
    private static boolean initialized = false;
    private static Map<String, Object> globalVariables = new HashMap<>();


    public static void init(Map<String, Object> variables) {
        if (!initialized) {
            SpringElUtil.globalVariables = variables;
            Object discoverer;
            try {
                Class<?> springNameDiscoverer = Class.forName("org.springframework.core.DefaultParameterNameDiscoverer");
                discoverer = springNameDiscoverer.newInstance();
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
                discoverer = new ReflectionParameterNameDiscoverer();
            }

            try {
                Method parameterNames = discoverer.getClass().getMethod("getParameterNames", Method.class);
                Object finalDiscoverer = discoverer;
                parameterNameDiscoverer = method -> {
                    try {
                        return (String[]) parameterNames.invoke(finalDiscoverer, method);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        return null;
                    }
                };
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
            initialized = true;
        }
    }


    public static SpringELEvaluationContext parse(String expression) {
        return new SpringELEvaluationContext(expression);
    }

    public static class SpringELEvaluationContext {

        private final String expression;

        private final Map<String, Object> variables = new HashMap<>();

        private Object executionContext;


        public SpringELEvaluationContext(String expression) {
            this.expression = expression;
        }

        public SpringELEvaluationContext addVar(String v, Object o) {
            variables.put(v, o);
            return this;
        }

        public SpringELEvaluationContext setExecutionContext(Object o) {
            this.executionContext = o;
            return this;
        }

        public SpringELEvaluationContext setMethodInvocationContext(Object target, Method method, Object[] args, Object result) {

            String[] parameterNames = parameterNameDiscoverer.apply(method);

            if (args != null) {
                for (int i = 0; i < args.length; i++) {
                    if (parameterNames != null) {
                        addVar(parameterNames[i], args[i]);
                        addVar(parameterNames[i] + "$each", args[i]);
                    }
                    addVar("arg" + i, args[i]);
                    addVar("arg" + i + "$each", args[i]);
                    addVar("param" + i, args[i]);
                    addVar("param" + i + "$each", args[i]);
                }
                addVar("args", args).addVar("params", args);
            }
            addVar("result", result);

            addVar("methodSignature", ReflectionUtil.getSignatureWithArgValues(method, args));
            return setExecutionContext(target);
        }

        public Object getValue() {
            return getValue(Object.class);
        }

        public <T> T getValue(Class<T> type) {
            if (initialized) {
                StandardEvaluationContext simpleContext = executionContext == null ? new StandardEvaluationContext() : new StandardEvaluationContext(executionContext);
                simpleContext.addMethodResolver(privateMethodResolver);
                simpleContext.addPropertyAccessor(privatePropertyAccessor);
                Map<String, Object> variablesCombined = new HashMap<>();
                variablesCombined.putAll(globalVariables);
                variablesCombined.putAll(variables);
                if (variablesCombined.size() != globalVariables.size() + variables.size()) {
                    throw new CacheOperationException("Conflict variables found in SpringEL context");
                }
                simpleContext.setVariables(variablesCombined);
                return parser.parseExpression(expression).getValue(simpleContext, type);
            } else {
                throw new CacheOperationException("SpringElUtil is not initialized");
            }
        }
    }

    static class CuFeRr extends ReflectivePropertyAccessor {
        @Override
        protected Field findField(String name, Class<?> clazz, boolean mustBeStatic) {
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                if (field.getName().equals(name) && (!mustBeStatic || Modifier.isStatic(field.getModifiers()))) {
                    return field;
                }
            }
            // We'll search superclasses and implemented interfaces explicitly,
            // although it shouldn't be necessary - however, see SPR-10125.
            if (clazz.getSuperclass() != null) {
                Field field = findField(name, clazz.getSuperclass(), mustBeStatic);
                if (field != null) {
                    return field;
                }
            }
            for (Class<?> implementedInterface : clazz.getInterfaces()) {
                Field field = findField(name, implementedInterface, mustBeStatic);
                if (field != null) {
                    return field;
                }
            }
            return null;
        }
    }

    static class CuMeRr extends ReflectiveMethodResolver {
        @Override
        protected Method[] getMethods(Class<?> type) {
            Method[] methods = type.getMethods();
            for (Method method : methods) {
                method.setAccessible(true);
            }
            Set<Method> methodList = new HashSet<>(Arrays.asList(methods));

            Method[] declaredMethods = type.getDeclaredMethods();
            for (Method method : declaredMethods) {
                method.setAccessible(true);
            }
            methodList.addAll(Arrays.asList(declaredMethods));
            return methodList.toArray(new Method[0]);
        }
    }


    static class ReflectionParameterNameDiscoverer {
        public String[] getParameterNames(Method method) {
            return getParameterNames(method.getParameters());
        }

        private String[] getParameterNames(Parameter[] parameters) {
            String[] parameterNames = new String[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                Parameter param = parameters[i];
                if (!param.isNamePresent()) {
                    return null;
                }
                parameterNames[i] = param.getName();
            }
            return parameterNames;
        }
    }


}
