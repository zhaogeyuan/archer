package com.atpex.archer;

import com.atpex.archer.annotation.Cache;
import com.atpex.archer.annotation.CacheList;
import com.atpex.archer.annotation.CacheMulti;
import com.atpex.archer.cache.CacheInitializerDelegate;
import com.atpex.archer.cache.api.CacheInitializer;
import com.atpex.archer.cache.api.CacheShard;
import com.atpex.archer.cache.internal.ShardingCache;
import com.atpex.archer.cache.internal.ShardingCacheConfigure;
import com.atpex.archer.components.api.KeyGenerator;
import com.atpex.archer.components.api.ValueSerializer;
import com.atpex.archer.components.internal.InternalObjectValueSerializer;
import com.atpex.archer.constants.Serialization;
import com.atpex.archer.exception.CacheBeanParsingException;
import com.atpex.archer.exception.CacheOperationException;
import com.atpex.archer.invocation.InvocationInterceptor;
import com.atpex.archer.metadata.EvictionMetadata;
import com.atpex.archer.metadata.ListCacheMetadata;
import com.atpex.archer.metadata.ObjectCacheMetadata;
import com.atpex.archer.stats.listener.CacheMetricsListener;
import com.atpex.archer.stats.listener.InternalCacheHitRateListener;
import com.atpex.archer.stats.observer.InternalCacheMetricsObserver;
import com.atpex.archer.operation.EvictionOperation;
import com.atpex.archer.operation.ListCacheOperation;
import com.atpex.archer.operation.ObjectCacheOperation;
import com.atpex.archer.util.CacheResolver;
import com.atpex.archer.util.ReflectionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The starter of archer cache framework
 *
 * @author atpexgo.wu
 * @since 1.0.0
 */
@SuppressWarnings("all")
public class Archer {

    public static final Logger logger = LoggerFactory.getLogger(Archer.class);

    /**
     * Use this map to avoid creating duplicated internal serializer
     */
    @SuppressWarnings("rawtypes")
    private final Map<String, InternalObjectValueSerializer> internalValueSerializers = new ConcurrentHashMap<>();


    private final Map<String, KeyGenerator> keyGeneratorMap = new ConcurrentHashMap<>();

    private final Map<String, ValueSerializer> valueSerializerMap = new ConcurrentHashMap<>();

    private final Set<CacheMetricsListener> metricsListeners = new ConcurrentSkipListSet<>();

    private final CacheManager cacheManager = new CacheManager();

    private CacheInitializer cacheInitializer;

    private final List<CacheShard> cacheShards = Collections.synchronizedList(new ArrayList<>());

    private Archer() {
    }

    public static Archer create(String... basePackages) {
        ReflectionUtil.forPackage(basePackages);
        return new Archer();
    }

    public Archer addValueSerializer(String name, ValueSerializer valueSerializer) {
        valueSerializerMap.put(name, valueSerializer);
        return this;
    }

    public Archer addKeyGenerator(String name, KeyGenerator keyGenerator) {
        keyGeneratorMap.put(name, keyGenerator);
        return this;
    }

    public Archer addMetricsListener(CacheMetricsListener metricsListener) {
        this.metricsListeners.add(metricsListener);
        return this;
    }

    public Archer setOperationConfigs(List<? extends CacheShard> configs) {
        cacheShards.addAll(configs);
        return this;
    }

    public Archer setCacheOperationInitializationProcessor(CacheInitializer processor) {
        this.cacheInitializer = processor;
        return this;
    }

    public Archer addOperationConfig(CacheShard shard) {
        cacheShards.add(shard);
        return this;
    }

    public Starter init() {
        cacheManager.getSerializerMap().putAll(valueSerializerMap);
        cacheManager.getKeyGeneratorMap().putAll(keyGeneratorMap);

        InternalCacheMetricsObserver cacheMetricsObserver = new InternalCacheMetricsObserver();
        for (CacheMetricsListener metricsListener : this.metricsListeners) {
            cacheMetricsObserver.register(metricsListener);
        }

        InternalCacheHitRateListener internalCacheHitRateListener = new InternalCacheHitRateListener();
        if (CacheManager.Config.metricsEnabled) {
            internalCacheHitRateListener.startPrint();
        }
        cacheMetricsObserver.register(internalCacheHitRateListener);

        cacheManager.setCacheObserver(cacheMetricsObserver);

        sharding();
        cacheManager.initialized();

        InvocationInterceptor.init(cacheManager);

        return new Starter(this);
    }

    public static void serialization(Serialization serialization) {
        CacheManager.Config.valueSerialization = serialization;
    }

    public static void enableMetrics() {
        CacheManager.Config.metricsEnabled = true;
    }

    public static void enableMetrics(boolean enable) {
        CacheManager.Config.metricsEnabled = enable;
    }

    private void eviction(Class<?> service, String signature, Method declaredMethod) {
        if (cacheManager.getMethodSignatureToOperationSourceName().containsKey(signature)) {
            return;
        }
        List<Annotation> cacheEvictAnnotations = ReflectionUtil.getRepeatableCacheAnnotations(declaredMethod);
        for (Annotation cacheEvictAnnotation : cacheEvictAnnotations) {
            EvictionMetadata metadata = (EvictionMetadata) CacheResolver.resolveMetadata(declaredMethod, cacheEvictAnnotation);
            EvictionOperation evictionOperation = new EvictionOperation();
            evictionOperation.setMetadata(metadata);
            evictionOperation.initialized();
            String name = "eviction" + UUID.randomUUID().toString();
            cacheManager.getEvictionOperationMap().put(name, evictionOperation);
            cacheManager.getMethodSignatureToOperationSourceName().computeIfAbsent(signature, s -> new ArrayList<>()).add(name);
        }
    }


    private void cacheable(Class<?> service, String signature, Method declaredMethod) {
        if (cacheManager.getMethodSignatureToOperationSourceName().containsKey(signature)) {
            return;
        }
        List<Annotation> annotations = ReflectionUtil.getCacheAnnotations(declaredMethod, Cache.class, CacheMulti.class);
        for (Annotation annotation : annotations) {
            if (annotation != null) {
                ObjectCacheMetadata metadata = (ObjectCacheMetadata) CacheResolver.resolveMetadata(declaredMethod, annotation);
                ObjectCacheOperation cacheOperation = new ObjectCacheOperation();
                cacheOperation.setMetadata(metadata);

                if (metadata.isMultiple()) {
                    cacheOperation.setMultipleLoader(CacheResolver.resolveMultiLoader(declaredMethod));
                } else {
                    cacheOperation.setLoader(CacheResolver.resolveSingleLoader(declaredMethod));
                }

                Type cacheEntityType = metadata.isMultiple() ? CacheResolver.parseCacheEntityType(declaredMethod) : declaredMethod.getGenericReturnType();
                if (CacheManager.Config.valueSerialization == Serialization.HESSIAN || CacheManager.Config.valueSerialization == Serialization.JAVA) {
                    if (!Serializable.class.isAssignableFrom(ReflectionUtil.toClass(cacheEntityType))) {
                        throw new CacheBeanParsingException("To use Hessian or Java serialization, " + cacheEntityType.getTypeName() + " must implement java.io.Serializable");
                    }
                }

                ValueSerializer userValueSerializer = StringUtils.isEmpty(metadata.getValueSerializer()) ? null : valueSerializerMap.getOrDefault(metadata.getValueSerializer(), null);
                if (userValueSerializer != null) {
                    cacheOperation.setValueSerializer(userValueSerializer);
                } else {
                    internalValueSerializers.computeIfAbsent(cacheEntityType.getTypeName(), new Function<String, InternalObjectValueSerializer>() {
                        @Override
                        public InternalObjectValueSerializer apply(String s) {
                            return new InternalObjectValueSerializer(cacheEntityType);
                        }
                    });
                    cacheOperation.setValueSerializer(
                            internalValueSerializers.get(cacheEntityType.getTypeName())
                    );
                }

                cacheOperation.initialized();
                String name = "cacheable" + UUID.randomUUID().toString();
                cacheManager.getCacheOperationMap().put(name, cacheOperation);
                cacheManager.getMethodSignatureToOperationSourceName().computeIfAbsent(signature, s -> new ArrayList<>()).add(name);
            }
        }
    }

    private void listCacheable(Class<?> service, String signature, Method declaredMethod) {
        if (cacheManager.getMethodSignatureToOperationSourceName().containsKey(signature)) {
            return;
        }
        List<Annotation> annotations = ReflectionUtil.getCacheAnnotations(declaredMethod, CacheList.class);
        for (Annotation annotation : annotations) {
            if (annotation != null) {
                ListCacheMetadata metadata = (ListCacheMetadata) CacheResolver.resolveMetadata(declaredMethod, annotation);
                ListCacheOperation listCacheOperation = new ListCacheOperation();
                listCacheOperation.setMetadata(metadata);
                listCacheOperation.setLoader(CacheResolver.createListableCacheLoader());
                Type cacheEntityType = CacheResolver.parseCacheEntityType(declaredMethod);
                if (CacheManager.Config.valueSerialization == Serialization.HESSIAN || CacheManager.Config.valueSerialization == Serialization.JAVA) {
                    if (!Serializable.class.isAssignableFrom(ReflectionUtil.toClass(cacheEntityType))) {
                        throw new CacheBeanParsingException("To use Hessian or Java serialization, " + cacheEntityType.getTypeName() + " must implement java.io.Serializable");
                    }
                }

                ValueSerializer userValueSerializer = StringUtils.isEmpty(metadata.getElementValueSerializer()) ? null : valueSerializerMap.getOrDefault(metadata.getElementValueSerializer(), null);
                if (userValueSerializer != null) {
                    listCacheOperation.setValueSerializer(userValueSerializer);
                } else {
                    internalValueSerializers.computeIfAbsent(cacheEntityType.getTypeName(), new Function<String, InternalObjectValueSerializer>() {
                        @Override
                        public InternalObjectValueSerializer apply(String s) {
                            return new InternalObjectValueSerializer(cacheEntityType);
                        }
                    });
                    listCacheOperation.setValueSerializer(
                            internalValueSerializers.get(cacheEntityType.getTypeName())
                    );
                }

                listCacheOperation.initialized();
                String name = "listCacheable" + UUID.randomUUID().toString();
                cacheManager.getCacheOperationMap().put(name, listCacheOperation);
                cacheManager.getMethodSignatureToOperationSourceName().computeIfAbsent(signature, s -> new ArrayList<>()).add(name);
            }
        }
    }

    private void sharding() {
        if (cacheInitializer == null) {
            cacheInitializer = new CacheInitializerDelegate();
        }
        ShardingCacheConfigure shardingCacheConfigure = new ShardingCacheConfigure(cacheInitializer, cacheShards);
        shardingCacheConfigure.init();
        ShardingCache shardingCache = new ShardingCache(shardingCacheConfigure);
        cacheManager.setShardingCache(shardingCache);
    }

    private Supplier<Object> methodInvoker(Object instance, Method method, Object[] args) {
        return () -> {
            try {
                method.setAccessible(true);
                return method.invoke(instance, args);
            } catch (IllegalAccessException | InvocationTargetException e) {
                logger.error("Can't invoke method " + method.getName(), e);
                return null;
            }
        };
    }


    public static class Starter {

        private Archer archer;

        private Starter(Archer archer) {
            this.archer = archer;
        }

        public <T> T start(Class<T> service) {
            T instance;
            try {
                instance = service.newInstance();
                return start(service, instance);
            } catch (InstantiationException | IllegalAccessException e) {
                logger.error("Can't create service instance.", e);
                return null;
            }
        }

        public <T> T start(Class<T> serviceType, T instance) {

            Method[] declaredMethods = instance.getClass().getMethods();
            for (Method declaredMethod : declaredMethods) {
                String signature = ReflectionUtil.getSignatureForCache(declaredMethod);
                archer.eviction(instance.getClass(), signature, declaredMethod);
                archer.cacheable(instance.getClass(), signature, declaredMethod);
                archer.listCacheable(instance.getClass(), signature, declaredMethod);
            }
            if (serviceType.isInterface()) {
                // JDK proxy
                InvocationHandler invocationHandler = (proxy, method, args) -> InvocationInterceptor.INSTANCE.invoke(proxy, instance, archer.methodInvoker(instance, method, args), method, args);
                return (T) Proxy.newProxyInstance(serviceType.getClassLoader(), new Class[]{serviceType}, invocationHandler);
            } else {
                // cglib proxy
                try {
                    Class<?> enhancerClass = Class.forName("net.sf.cglib.proxy.Enhancer");
                    Object enhancer = enhancerClass.newInstance();
                    Method setSuperclass = enhancerClass.getDeclaredMethod("setSuperclass", Class.class);
                    setSuperclass.invoke(enhancer, serviceType);

                    Class<?> callBackClass = Class.forName("net.sf.cglib.proxy.Callback");
                    Method setCallback = enhancerClass.getDeclaredMethod("setCallback", callBackClass);

                    Class<?> methodCallBackClass = Class.forName("net.sf.cglib.proxy.MethodInterceptor");

                    InvocationHandler anonymousMethodCallback = (p, m, a) -> {
                        Object target = a[0];
                        Method method = (Method) a[1];
                        Object[] args = (Object[]) a[2];
                        return InvocationInterceptor.INSTANCE.invoke(target, instance, archer.methodInvoker(instance, method, args), method, args);
                    };
                    setCallback.invoke(enhancer, Proxy.newProxyInstance(methodCallBackClass.getClassLoader(), new Class[]{methodCallBackClass}, anonymousMethodCallback));

                    Method create = enhancerClass.getDeclaredMethod("create");
                    return (T) create.invoke(enhancer);
                } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | NoSuchMethodException | InvocationTargetException e) {
                    logger.error("To user class not implement interface, you should involve cglib dependency.");
                    throw new CacheOperationException(e);
                }
            }
        }
    }

}