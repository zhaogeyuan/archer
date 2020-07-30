package com.github.attt.archer.invocation;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.parser.ParserConfig;
import com.github.attt.archer.CacheManager;
import com.github.attt.archer.annotation.extra.HashKey;
import com.github.attt.archer.exception.FallbackException;
import com.github.attt.archer.metadata.EvictionMetadata;
import com.github.attt.archer.metadata.ObjectCacheMetadata;
import com.github.attt.archer.metadata.api.AbstractCacheMetadata;
import com.github.attt.archer.operation.CacheOperation;
import com.github.attt.archer.operation.EvictionOperation;
import com.github.attt.archer.operation.ListCacheOperation;
import com.github.attt.archer.processor.ListProcessor;
import com.github.attt.archer.processor.api.AbstractProcessor;
import com.github.attt.archer.processor.context.InvocationContext;
import com.github.attt.archer.util.CommonUtils;
import com.github.attt.archer.util.ReflectionUtil;
import com.github.attt.archer.util.SpringElUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Supplier;

/**
 * Invocation interceptor
 *
 * @author atpexgo.wu
 * @since 1.0
 */
@SuppressWarnings("all")
public class InvocationInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(InvocationInterceptor.class);

    private CacheManager manager;

    public static final InvocationInterceptor INSTANCE = new InvocationInterceptor();

    InvocationInterceptor() {
    }

    public static void init(CacheManager manager) {
        CacheContext.setCacheManager(manager);
        INSTANCE.manager = manager;
    }

    public Object invoke(Object proxy, Object target, Supplier<?> methodInvoker, Method method, Object[] args) throws Throwable {

        // proxy in current thread
        CacheContext.setCurrentProxy(proxy);

        // get the actual method of target object invoked
        method = target.getClass().getMethod(method.getName(), method.getParameterTypes());

        if (!ReflectionUtil.findAnyCacheAnnotation(method)) {
            return methodInvoker.get();
        }

        String methodSignature = ReflectionUtil.getSignature(method, true, true);
        // cache management

        // cache acceptation operation sources
        List<CacheOperation> cacheOperations = manager.getCacheOperations(methodSignature, CacheOperation.class);

        // cache eviction operation sources
        List<EvictionOperation> evictionOperations = manager.getEvictionOperations(methodSignature);

        // merge cache context
        AcceptationContext acceptationContext = new AcceptationContext();
        for (CacheOperation cacheOperation : cacheOperations) {
            // cache config metadata
            AbstractCacheMetadata metadata = cacheOperation.getMetadata();

            // service cache
            AbstractProcessor processor = manager.getProcessor(cacheOperation);

            if (conditionPass(metadata.getCondition(), target, method, args)) {
                logger.debug("Condition check passed, enter service cache procedure");

                acceptationContext.merge(
                        metadata instanceof ObjectCacheMetadata && ((ObjectCacheMetadata) metadata).isMultiple() ?
                                multiCacheProcessor(processor, cacheOperation, target, method, args, methodInvoker) :
                                cacheProcessor(processor, cacheOperation, target, method, args, methodInvoker)
                );
            }
        }

        // merge eviction context
        EvictionContext evictionContext = new EvictionContext();
        for (EvictionOperation evictionOperation : evictionOperations) {
            evictionContext.merge(
                    evictionProcessor(manager, manager.getCacheOperationMap().values(), evictionOperation, target, method, args)
            );
        }

        // init invoked event & get observer
        // proceed

        // proceed eviction pre-operations
        for (Supplier<?> preOperation : evictionContext.preOperations) {
            preOperation.get();
        }

        Object result = acceptationContext.proceed(methodInvoker);

        // proceed eviction post-operations
        for (Supplier<?> postOperation : evictionContext.postOperations) {
            postOperation.get();
        }
        return result;
    }

    /**
     * Check SpringEL Expression formed condition
     *
     * @param condition
     * @param target
     * @param method
     * @param args
     * @return true if pass
     */
    private boolean conditionPass(String condition, Object target, Method method, Object[] args) {
        if (!CommonUtils.isEmpty(condition)) {
            return SpringElUtil.parse(condition).setMethodInvocationContext(target, method, args, null).getValue(Boolean.class);
        }
        return true;
    }

    /**
     * Create cache context which contains intercepted cache operations
     *
     * @param cacheProcessor
     * @param cacheOperation
     * @param target
     * @param method
     * @param args
     * @param methodInvoker
     * @return
     * @throws Throwable
     */
    private AcceptationContext multiCacheProcessor(AbstractProcessor cacheProcessor,
                                                   CacheOperation cacheOperation,
                                                   Object target, Method method, Object[] args, Supplier<?> methodInvoker) throws Throwable {
        AcceptationContext acceptationContext = new AcceptationContext();

        List<InvocationContext> contexts = new ArrayList<>();

        List<Object[]> newArgs = ReflectionUtil.flattenArgs(args);

        for (Object[] newArg : newArgs) {
            InvocationContext context = new InvocationContext();
            context.setTarget(target);
            context.setMethod(method);
            context.setArgs(newArg);
            context.setMethodInvoker(methodInvoker);
            contexts.add(context);
        }

        push(acceptationContext.operations, () -> {
            try {
                Map all = cacheProcessor.getAll(contexts, cacheOperation);
                Object[] values = all.values().toArray();
                if (all instanceof LinkedHashMap) {
                    Object[] keys = all.keySet().toArray(new InvocationContext[0]);
                    values = new Object[keys.length];
                    for (int i = 0; i < keys.length; i++) {
                        values[i] = all.get(keys[i]);
                    }
                }

                Map hash = toHash(Arrays.asList(values), target, method, args);
                if (hash != null) {
                    return hash;
                }

                ReflectionUtil.makeAccessible(values.getClass().getComponentType());
                return JSON.parseObject(JSON.toJSONString(values), method.getGenericReturnType(), new ParserConfig(true), Feature.SupportNonPublicField);
            } catch (FallbackException fallbackException) {
                return methodInvoker.get();
            } finally {
            }
        }, false);
        return acceptationContext;
    }

    /**
     * Create cache context which contains intercepted cache operations
     *
     * @param cacheProcessor
     * @param operationSource
     * @param target
     * @param method
     * @param args
     * @param methodInvoker
     * @return
     * @throws Throwable
     */
    private AcceptationContext cacheProcessor(AbstractProcessor cacheProcessor,
                                              CacheOperation cacheOperation,
                                              Object target, Method method, Object[] args, Supplier<?> methodInvoker) throws Throwable {
        AcceptationContext acceptationContext = new AcceptationContext();
        InvocationContext context = new InvocationContext();
        context.setTarget(target);
        context.setMethod(method);
        context.setArgs(args);
        context.setMethodInvoker(methodInvoker);

        push(acceptationContext.operations, () -> {
            try {
                Object result = cacheProcessor.get(context, cacheOperation);
                if (result != null && cacheProcessor instanceof ListProcessor) {
                    // fix real type if is collection or array
                    Type componentOrArgumentType = ReflectionUtil.getComponentOrArgumentType(method);
                    if (componentOrArgumentType != null) {
                        ReflectionUtil.makeAccessible(componentOrArgumentType);
                    }

                    Map hash = toHash((Collection) result, target, method, args);
                    if (hash != null) {
                        return hash;
                    }

                    result = JSON.parseObject(JSON.toJSONString(result), method.getGenericReturnType(), new ParserConfig(true), Feature.SupportNonPublicField);
                }
                return result;
            } catch (FallbackException fallbackException) {
                return methodInvoker.get();
            } finally {
            }
        }, false);
        return acceptationContext;
    }


    private Map toHash(Iterable result, Object target, Method method, Object[] args) {
        List annotations = ReflectionUtil.getCacheAnnotations(method, HashKey.class);
        if (CommonUtils.isNotEmpty(annotations)) {
            HashKey hashKey = (HashKey) annotations.get(0);
            Map resultHash = new HashMap();
            SpringElUtil.SpringELEvaluationContext springELEvaluationContext = SpringElUtil.parse(hashKey.value());
            for (Object o : result) {
                if (o == null) {
                    continue;
                }
                Object key = springELEvaluationContext.setMethodInvocationContext(
                        target,
                        method,
                        args,
                        result
                ).addVar("result$each", o).getValue();

                if (key != null) {
                    resultHash.put(key, o);
                }
            }
            return resultHash;
        }
        return null;
    }

    /**
     * Create eviction context which contains intercepted eviction operations
     *
     * @param management
     * @param cacheOperations
     * @param evictionOperation
     * @param target
     * @param method
     * @param args
     * @return
     * @throws Throwable
     */
    private EvictionContext evictionProcessor(CacheManager management,
                                              Collection<CacheOperation> cacheOperations,
                                              EvictionOperation evictionOperation,
                                              Object target, Method method, Object[] args) throws Throwable {

        EvictionContext evictionContext = new EvictionContext();
        EvictionMetadata metadata = evictionOperation.getMetadata();
        List<Supplier<?>> operations = metadata.getAfterInvocation() ? evictionContext.postOperations : evictionContext.preOperations;
        for (CacheOperation cacheOperation : cacheOperations) {
            AbstractProcessor processor = management.getProcessor(cacheOperation);
            Supplier eviction = null;
            if (metadata.getMultiple()) {
                List<InvocationContext> contexts = new ArrayList<>();
                List<Object[]> newArgs = ReflectionUtil.flattenArgs(args);
                for (Object[] newArg : newArgs) {
                    InvocationContext context = new InvocationContext();
                    context.setTarget(target);
                    context.setMethod(method);
                    context.setArgs(newArg);
                    contexts.add(context);
                }
                eviction = () -> {
                    try {
                        logger.debug("Delete contexts: {}", contexts);
                        processor.deleteAll(contexts, cacheOperation);
                    } finally {
                    }
                    return null;
                };
            } else {
                InvocationContext context = new InvocationContext();
                context.setTarget(target);
                context.setMethod(method);
                context.setArgs(args);
                eviction = () -> {
                    try {
                        logger.debug("Delete context: {}", context);
                        processor.delete(context, cacheOperation);
                    } finally {
                    }
                    return null;
                };
            }

            // always delete list cache first, because complex cache may dose not just delete key, also need cache key to find some other records
            push(operations, eviction, cacheOperation instanceof ListCacheOperation);
        }
        return evictionContext;
    }

    /**
     * push operation to operations chain
     *
     * @param operations
     * @param operation
     * @param highPriority true if operation needs to executed in first priority
     */
    private void push(List<Supplier<?>> operations, Supplier<?> operation, boolean highPriority) {
        if (highPriority) {
            operations.add(0, operation);
        } else {
            operations.add(operation);
        }
    }


    static class EvictionContext {
        List<Supplier<?>> preOperations = new ArrayList<>();
        List<Supplier<?>> postOperations = new ArrayList<>();

        void merge(EvictionContext evictionContext) {
            preOperations.addAll(evictionContext.preOperations);
            postOperations.addAll(evictionContext.postOperations);
        }
    }

    static class AcceptationContext {
        List<Supplier<?>> operations = new ArrayList<>();

        void merge(AcceptationContext acceptationContext) {
            operations.addAll(acceptationContext.operations);
        }

        Object proceed(Supplier<?> methodInvoker) {
            if (CommonUtils.isEmpty(operations)) {
                return methodInvoker.get();
            }
            Object result = null;
            for (Supplier<?> operation : operations) {
                result = operation.get();
            }
            return result;
        }
    }
}
