package com.atpex.archer.invocation;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.parser.ParserConfig;
import com.himalaya.service.cacheable.cache.management.CacheManagement;
import com.himalaya.service.cacheable.cache.metadata.AbstractCacheAcceptationMetadata;
import com.himalaya.service.cacheable.cache.metadata.AbstractCacheMetadata;
import com.himalaya.service.cacheable.cache.metadata.impl.CacheEvictionMetadata;
import com.himalaya.service.cacheable.cache.metadata.impl.ObjectCacheMetadata;
import com.himalaya.service.cacheable.cache.processor.AbstractServiceCacheProcessor;
import com.himalaya.service.cacheable.cache.processor.ListableServiceCacheProcessor;
import com.himalaya.service.cacheable.cache.source.AbstractAcceptationOperationSource;
import com.himalaya.service.cacheable.cache.source.AbstractListableOperationSource;
import com.himalaya.service.cacheable.cache.source.impl.EvictionOperationSource;
import com.himalaya.service.cacheable.context.CacheInvocationContext;
import com.himalaya.service.cacheable.context.CacheInvocationContexts;
import com.himalaya.service.cacheable.exceptions.FallbackException;
import com.himalaya.service.cacheable.metrics.event.CacheHitRateEvent;
import com.himalaya.service.cacheable.metrics.event.CacheInvokedEvent;
import com.himalaya.service.cacheable.metrics.observer.CacheMetricsObserver;
import com.himalaya.service.cacheable.util.CommonUtils;
import com.himalaya.service.cacheable.util.ReflectionUtil;
import com.himalaya.service.cacheable.util.SpringElUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Invocation interceptor
 *
 * @author atpexgo.wu
 * @since 1.0.0
 */
@SuppressWarnings("all")
public class InvocationInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(InvocationInterceptor.class);

    private CacheManagement management;

    private CacheMetricsObserver<CacheInvokedEvent> metricsObserver;

    public static final InvocationInterceptor INSTANCE = new InvocationInterceptor();

    InvocationInterceptor() {
    }

    public static void init(CacheManagement management, CacheMetricsObserver<CacheInvokedEvent> metricsObserver) {
        CacheContext.setCacheManager(management);
        INSTANCE.management = management;
        INSTANCE.metricsObserver = metricsObserver;
    }

    public Object invoke(Object proxy, Object target, Supplier<?> methodInvoker, Method method, Object[] args) throws Throwable {

        // proxy in current thread
        CacheContext.setCurrentProxy(proxy);

        // get the actual method of target object invoked
        method = target.getClass().getMethod(method.getName(), method.getParameterTypes());

        if (!ReflectionUtil.findAnyCacheableAnnotation(method)) {
            return methodInvoker.get();
        }

        String methodSignature = ReflectionUtil.getSignature(method, true, true);
        // cache management

        // cache acceptation operation sources
        List<AbstractAcceptationOperationSource> acceptationOperationSources = management.getAcceptationOperationSources(methodSignature, AbstractAcceptationOperationSource.class);

        // cache eviction operation sources
        List<EvictionOperationSource> evictionOperationSources = management.getEvictionOperationSources(methodSignature);

        // merge cache context
        AcceptationContext acceptationContext = new AcceptationContext();
        for (AbstractAcceptationOperationSource cacheAcceptationOperationSource : acceptationOperationSources) {
            // cache config metadata
            AbstractCacheMetadata metadata = cacheAcceptationOperationSource.getMetadata();

            // service cache
            AbstractServiceCacheProcessor serviceCache = management.getServiceCache(cacheAcceptationOperationSource);

            if (conditionPass(metadata.getCondition(), target, method, args)) {
                logger.debug("Condition check passed, enter service cache procedure");

                acceptationContext.merge(
                        metadata instanceof ObjectCacheMetadata && ((ObjectCacheMetadata) metadata).isMultiple() ?
                                multiCacheProcessor(serviceCache, cacheAcceptationOperationSource, target, method, args, methodInvoker) :
                                cacheProcessor(serviceCache, cacheAcceptationOperationSource, target, method, args, methodInvoker)
                );
            }
        }

        // merge eviction context
        EvictionContext evictionContext = new EvictionContext();
        for (EvictionOperationSource evictionOperationSource : evictionOperationSources) {
            evictionContext.merge(
                    evictionProcessor(management, management.getAcceptationOperationSourceMap().values(), evictionOperationSource, target, method, args)
            );
        }

        // init invoked event & get observer
        CacheInvokedEvent cacheInvokedEvent = new CacheInvokedEvent();
        // proceed

        // proceed eviction pre-operations
        for (Supplier<?> preOperation : evictionContext.preOperations) {
            cacheInvokedEvent.setInvoked(true);
            preOperation.get();
        }

        Object result = acceptationContext.proceed((none) -> cacheInvokedEvent.setInvoked(true), methodInvoker);

        // proceed eviction post-operations
        for (Supplier<?> postOperation : evictionContext.postOperations) {
            cacheInvokedEvent.setInvoked(true);
            postOperation.get();
        }
        metricsObserver.observe(cacheInvokedEvent);
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
     * @param operationSource
     * @param target
     * @param method
     * @param args
     * @param methodInvoker
     * @return
     * @throws Throwable
     */
    private AcceptationContext multiCacheProcessor(AbstractServiceCacheProcessor cacheProcessor,
                                                   AbstractAcceptationOperationSource operationSource,
                                                   Object target, Method method, Object[] args, Supplier<?> methodInvoker) throws Throwable {
        AcceptationContext acceptationContext = new AcceptationContext();

        List<CacheInvocationContext> contexts = new ArrayList<>();

        List<Object[]> newArgs = ReflectionUtil.flattenArgs(args);

        for (Object[] newArg : newArgs) {
            CacheInvocationContext context = CacheInvocationContext.builder()
                    .method(method)
                    .args(newArg)
                    .target(target)
                    .methodInvoker(methodInvoker)
                    .build();
            contexts.add(context);
        }

        CacheInvocationContexts cacheInvocationContexts = new CacheInvocationContexts();
        cacheInvocationContexts.setCacheInvocationContextList(contexts);
        cacheInvocationContexts.setEventBuilder(CacheHitRateEvent.eventBuilder(operationSource.getCacheType()).setTotalDataSize(contexts.size()));

        push(acceptationContext.operations, () -> {
            try {
                Map all = cacheProcessor.getAll(cacheInvocationContexts, operationSource);
                Object[] values = all.values().toArray();
                if (all instanceof LinkedHashMap) {
                    Object[] keys = all.keySet().toArray(new CacheInvocationContext[0]);
                    values = new Object[keys.length];
                    for (int i = 0; i < keys.length; i++) {
                        values[i] = all.get(keys[i]);
                    }
                }
                ReflectionUtil.makeAccessible(values.getClass().getComponentType());
                return JSON.parseObject(JSON.toJSONString(values), method.getGenericReturnType(), new ParserConfig(true), Feature.SupportNonPublicField);
            } catch (FallbackException fallbackException) {
                return methodInvoker.get();
            } finally {
                cacheProcessor.getCacheManagement().getCacheObserver().observe(cacheInvocationContexts.getEventBuilder().build());
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
    private AcceptationContext cacheProcessor(AbstractServiceCacheProcessor cacheProcessor,
                                              AbstractAcceptationOperationSource operationSource,
                                              Object target, Method method, Object[] args, Supplier<?> methodInvoker) throws Throwable {
        AcceptationContext acceptationContext = new AcceptationContext();
        CacheInvocationContext context =
                CacheInvocationContext.builder()
                        .method(method)
                        .args(args)
                        .target(target)
                        .methodInvoker(methodInvoker)
                        .build();
        context.setEventBuilder(CacheHitRateEvent.eventBuilder(operationSource.getCacheType()).setTotalDataSize(1));

        push(acceptationContext.operations, () -> {
            try {
                Object result = cacheProcessor.get(context, operationSource);
                if (result != null && cacheProcessor instanceof ListableServiceCacheProcessor) {
                    // fix real type if is collection or array
                    Type componentOrArgumentType = ReflectionUtil.getComponentOrArgumentType(method);
                    if (componentOrArgumentType != null) {
                        ReflectionUtil.makeAccessible(componentOrArgumentType);
                    }
                    result = JSON.parseObject(JSON.toJSONString(result), method.getGenericReturnType(), new ParserConfig(true), Feature.SupportNonPublicField);
                }
                return result;
            } catch (FallbackException fallbackException) {
                return methodInvoker.get();
            } finally {
                cacheProcessor.getCacheManagement().getCacheObserver().observe(context.getEventBuilder().build());
            }
        }, false);
        return acceptationContext;
    }

    /**
     * Create eviction context which contains intercepted eviction operations
     *
     * @param management
     * @param cacheAcceptationOperationSources
     * @param evictionOperationSource
     * @param target
     * @param method
     * @param args
     * @return
     * @throws Throwable
     */
    private EvictionContext evictionProcessor(CacheManagement management,
                                              Collection<AbstractAcceptationOperationSource> cacheAcceptationOperationSources,
                                              EvictionOperationSource evictionOperationSource,
                                              Object target, Method method, Object[] args) throws Throwable {

        EvictionContext evictionContext = new EvictionContext();
        CacheEvictionMetadata metadata = evictionOperationSource.getMetadata();
        List<Supplier<?>> operations = metadata.getAfterInvocation() ? evictionContext.postOperations : evictionContext.preOperations;
        for (AbstractAcceptationOperationSource cacheAcceptationOperationSource : cacheAcceptationOperationSources) {
            AbstractServiceCacheProcessor<Object, AbstractAcceptationOperationSource<AbstractCacheAcceptationMetadata, Object>> serviceCache = management.getServiceCache(cacheAcceptationOperationSource);
            if (Objects.equals(cacheAcceptationOperationSource.getMetadata().getArea(), metadata.getArea())) {

                CacheInvocationContext context = CacheInvocationContext.builder()
                        .target(target)
                        .method(method)
                        .args(args)
                        .build();
                context.setEventBuilder(CacheHitRateEvent.eventBuilder(evictionOperationSource.getCacheType()).setTotalDataSize(1));
                // always delete listable cache first, because complex cache dose not just delete key, also need cache key to find some other records
                push(operations, () -> {
                    try {
                        logger.debug("Delete context: {}", context);
                        serviceCache.delete(context, cacheAcceptationOperationSource);
                    } finally {
                        serviceCache.getCacheManagement().getCacheObserver().observe(context.getEventBuilder().build());
                    }
                    return null;
                }, cacheAcceptationOperationSource instanceof AbstractListableOperationSource);
            }
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

        Object proceed(Consumer<?> invokeMonitor, Supplier<?> methodInvoker) {
            if (CommonUtils.isEmpty(operations)) {
                return methodInvoker.get();
            }
            Object result = null;
            for (Supplier<?> operation : operations) {
                invokeMonitor.accept(null);
                result = operation.get();
            }
            return result;
        }
    }
}
