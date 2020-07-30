package com.github.attt.archer.spring.config;


import com.github.attt.archer.CacheManager;
import com.github.attt.archer.cache.internal.ShardingCache;
import com.github.attt.archer.components.api.KeyGenerator;
import com.github.attt.archer.components.api.Serializer;
import com.github.attt.archer.components.internal.InternalObjectValueSerializer;
import com.github.attt.archer.constants.Serialization;
import com.github.attt.archer.exception.CacheBeanParsingException;
import com.github.attt.archer.invocation.InvocationInterceptor;
import com.github.attt.archer.loader.SingleLoader;
import com.github.attt.archer.metadata.EvictionMetadata;
import com.github.attt.archer.metadata.ListCacheMetadata;
import com.github.attt.archer.metadata.ObjectCacheMetadata;
import com.github.attt.archer.operation.CacheOperation;
import com.github.attt.archer.operation.EvictionOperation;
import com.github.attt.archer.operation.ListCacheOperation;
import com.github.attt.archer.operation.ObjectCacheOperation;
import com.github.attt.archer.stats.api.CacheEvent;
import com.github.attt.archer.stats.api.listener.CacheStatsListener;
import com.github.attt.archer.stats.collector.NamedCacheEventCollector;
import com.github.attt.archer.util.CacheResolver;
import com.github.attt.archer.util.CommonUtils;
import com.github.attt.archer.util.ReflectionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.*;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache bean definition registry processor
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public class CacheBeanDefinitionRegistryProcessor implements BeanDefinitionRegistryPostProcessor, BeanClassLoaderAware {

    private static final Logger logger = LoggerFactory.getLogger(CacheBeanDefinitionRegistryProcessor.class);

    private final BeanNameGenerator beanNameGenerator = new DefaultBeanNameGenerator();

    /**
     * Use this map to avoid to create duplicated internal serializer
     */
    @SuppressWarnings("rawtypes")
    private final Map<String, InternalObjectValueSerializer> internalValueSerializers = new ConcurrentHashMap<>();

    /**
     * Use this map to save method signature mapping to operation source bean name
     * It will be passed to {@link CacheManager} bean after all operation source
     * registered.
     */
    private final Map<String, List<String>> methodSignatureToOperationSourceName = new ConcurrentHashMap<>();

    private ClassLoader classLoader;

    @Override
    public void postProcessBeanDefinitionRegistry(final BeanDefinitionRegistry registry) throws BeansException {
        try {
            registerCache(registry);
        } catch (Exception e) {
            throw new CacheBeanParsingException(e.getMessage(), e);
        }
    }

    /**
     * Iterate bean definitions and register
     * <p>
     * cache operation source
     * cache management
     * cache handler
     *
     * @param registry
     * @throws ClassNotFoundException
     */
    private void registerCache(final BeanDefinitionRegistry registry) throws ClassNotFoundException {

        String[] beanNames = registry.getBeanDefinitionNames();
        for (final String beanName : beanNames) {
            AbstractBeanDefinition beanDefinition = (AbstractBeanDefinition) registry.getBeanDefinition(beanName);
            Class<?> clazz;
            if (beanDefinition.hasBeanClass()) {
                clazz = beanDefinition.getBeanClass();
            } else {
                clazz = beanDefinition.resolveBeanClass(classLoader);
            }

            if (clazz != null) {
                Method[] methods = clazz.getMethods();
                for (final Method method : methods) {
                    List<Annotation> cacheAnnotations = ReflectionUtil.getCacheAnnotations(method);
                    for (Annotation annotation : cacheAnnotations) {
                        switch (ReflectionUtil.typeOf(annotation)) {
                            case OBJECT:
                                registerObjectCacheOperationSource(method, annotation, registry);
                                break;
                            case LIST:
                                registerListCacheOperationSource(method, annotation, registry);
                                break;
                            case EVICT:
                                List<Annotation> annotations = ReflectionUtil.getRepeatableCacheAnnotations(method);
                                if (annotations.size() > 0) {
                                    registerEvictionCacheOperationSource(method, annotations, registry);
                                }
                                break;
                            case NULL:
                            default:
                                break;
                        }
                    }
                }
            }
        }
    }

    private void registerObjectCacheOperationSource(
            final Method method,
            final Annotation annotation,
            final BeanDefinitionRegistry registry
    ) {
        if (annotation == null) {
            return;
        }
        final String methodSignature = ReflectionUtil.getSignature(method, true, true);

        final Type returnType = method.getGenericReturnType();

        final ObjectCacheMetadata metadata = (ObjectCacheMetadata) CacheResolver.resolveMetadata(method, annotation);

        Type cacheEntityType = metadata.isMultiple() ? CacheResolver.parseCacheEntityType(method) : returnType;

        if (CacheManager.Config.valueSerialization == Serialization.HESSIAN || CacheManager.Config.valueSerialization == Serialization.JAVA) {
            if (!Serializable.class.isAssignableFrom(ReflectionUtil.toClass(cacheEntityType))) {
                throw new CacheBeanParsingException("To use Hessian or Java serialization, " + cacheEntityType.getTypeName() + " must implement java.io.Serializable");
            }
        }

        logger.debug("CacheClass is : {}", cacheEntityType.getTypeName());

        // value serializer
        final String userValueSerializer = metadata.getValueSerializer();
        Object valueSerializer;
        if (CommonUtils.isEmpty(userValueSerializer)) {
            valueSerializer = internalValueSerializers.getOrDefault(cacheEntityType.getTypeName(),
                    new InternalObjectValueSerializer<>(cacheEntityType));
        } else {
            valueSerializer = new RuntimeBeanReference(userValueSerializer);
        }

        // register cache operation source bean definition
        AbstractBeanDefinition cacheOperationDefinition = BeanDefinitionBuilder.genericBeanDefinition(ObjectCacheOperation.class)
                .addPropertyValue("metadata", metadata)
                .addPropertyValue("loader", metadata.isMultiple() ? null : CacheResolver.resolveSingleLoader(method))
                .addPropertyValue("multipleLoader", metadata.isMultiple() ? CacheResolver.resolveMultiLoader(method) : null)
                .addPropertyValue("valueSerializer", valueSerializer)
                .addPropertyValue("cacheEventCollector", new NamedCacheEventCollector(metadata.getMethodSignature()))
                .getBeanDefinition();

        String operationName = beanNameGenerator.generateBeanName(cacheOperationDefinition, registry);
        registry.registerBeanDefinition(operationName, cacheOperationDefinition);

        // mapping method to operation source bean
        methodSignatureToOperationSourceName.computeIfAbsent(methodSignature, sigKey -> new ArrayList<>()).add(operationName);
    }


    private void registerListCacheOperationSource(
            final Method method,
            final Annotation annotation,
            final BeanDefinitionRegistry registry) {
        if (annotation == null) {
            return;
        }

        if (!ReflectionUtil.isCollectionOrArray(method.getReturnType())) {
            throw new CacheBeanParsingException("Listable cacheable method return type should be an array or Collection!");
        }

        final String methodSignature = ReflectionUtil.getSignature(method, true, true);

        final Type cacheEntityType = CacheResolver.parseCacheEntityType(method);

        if (CacheManager.Config.valueSerialization == Serialization.HESSIAN || CacheManager.Config.valueSerialization == Serialization.JAVA) {
            if (!Serializable.class.isAssignableFrom(ReflectionUtil.toClass(cacheEntityType))) {
                throw new CacheBeanParsingException("To use Hessian or Java serialization, " + cacheEntityType.getTypeName() + " must implement java.io.Serializable");
            }
        }

        final ListCacheMetadata metadata = (ListCacheMetadata) CacheResolver.resolveMetadata(method, annotation);

        logger.debug("CacheEntityClass is : {}", cacheEntityType.getTypeName());

        // create loader proxy
        SingleLoader<?> loader = CacheResolver.createListableCacheLoader();

        // operation source class
        BeanDefinitionBuilder cacheOperationDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(ListCacheOperation.class);

        // value serializer
        final String userElementValueSerializer = metadata.getElementValueSerializer();
        Object valueSerializer;
        if (CommonUtils.isEmpty(userElementValueSerializer)) {
            valueSerializer = internalValueSerializers.getOrDefault(cacheEntityType.getTypeName(),
                    new InternalObjectValueSerializer<>(cacheEntityType));
        } else {
            valueSerializer = new RuntimeBeanReference(userElementValueSerializer);
        }

        // register cache operation source bean definition
        AbstractBeanDefinition cacheOperationDefinition = cacheOperationDefinitionBuilder
                .addPropertyValue("metadata", metadata)
                .addPropertyValue("loader", loader)
                .addPropertyValue("valueSerializer", valueSerializer)
                .addPropertyValue("cacheEventCollector", new NamedCacheEventCollector(metadata.getMethodSignature()))
                .getBeanDefinition();

        String operationName = beanNameGenerator.generateBeanName(cacheOperationDefinition, registry);
        registry.registerBeanDefinition(operationName, cacheOperationDefinition);

        // mapping method to operation source bean
        methodSignatureToOperationSourceName.computeIfAbsent(methodSignature, sigKey -> new ArrayList<>()).add(operationName);
    }

    private void registerEvictionCacheOperationSource(
            final Method method,
            final List<Annotation> annotationList,
            final BeanDefinitionRegistry registry) {
        final String methodSignature = ReflectionUtil.getSignature(method, true, true);
        for (Annotation annotation : annotationList) {
            EvictionMetadata metadata = (EvictionMetadata) CacheResolver.resolveMetadata(method, annotation);

            // register cache OperationSource bean definition
            AbstractBeanDefinition cacheEvictionOperationSourceDefinition = BeanDefinitionBuilder.genericBeanDefinition(EvictionOperation.class)
                    .addPropertyValue("metadata", metadata)
                    .addPropertyValue("cacheEventCollector", new NamedCacheEventCollector(metadata.getMethodSignature()))
                    .getBeanDefinition();

            String operationSourceName = beanNameGenerator.generateBeanName(cacheEvictionOperationSourceDefinition, registry);
            registry.registerBeanDefinition(operationSourceName, cacheEvictionOperationSourceDefinition);

            // mapping method to OperationSource bean
            methodSignatureToOperationSourceName.computeIfAbsent(methodSignature, sigKey -> new ArrayList<>()).add(operationSourceName);
        }
    }


    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        beanFactory.addBeanPostProcessor(new CacheBeanPostProcessor());

        // register cache manager
        CacheManager cacheManager = new CacheManager();
        // pass reference to cacheManager
        cacheManager.setMethodSignatureToOperationSourceName(methodSignatureToOperationSourceName);

        Map<String, KeyGenerator> keyGeneratorMap = beanFactory.getBeansOfType(KeyGenerator.class);
        cacheManager.setKeyGeneratorMap(keyGeneratorMap);

        Map<String, Serializer> serializerMap = beanFactory.getBeansOfType(Serializer.class);
        cacheManager.setSerializerMap(serializerMap);

        // sharding cache, it's a sharding delegate for all caches
        ShardingCache shardingCache = beanFactory.getBean(ShardingCache.SHARDING_CACHE_BEAN_NAME, ShardingCache.class);
        cacheManager.setShardingCache(shardingCache);

        // cache operations
        Map<String, CacheOperation> cacheOperationMap = beanFactory.getBeansOfType(CacheOperation.class);
        Map<String, EvictionOperation> evictionOperationMap = beanFactory.getBeansOfType(EvictionOperation.class);

        // register cache stats listeners
        if (CacheManager.Config.metricsEnabled) {

            Map<String, CacheStatsListener> statsListenerMap = beanFactory.getBeansOfType(CacheStatsListener.class);
            cacheManager.setStatsListenerMap(statsListenerMap);

            for (CacheOperation cacheOperation : cacheOperationMap.values()) {
                for (CacheStatsListener<CacheEvent> statsListener : statsListenerMap.values()) {
                    cacheOperation.getCacheEventCollector().register(statsListener);
                }
            }

            for (EvictionOperation evictionOperation : evictionOperationMap.values()) {
                for (CacheStatsListener<CacheEvent> statsListener : statsListenerMap.values()) {
                    evictionOperation.getCacheEventCollector().register(statsListener);
                }
            }
        }

        cacheManager.setCacheOperationMap(cacheOperationMap);
        cacheManager.setEvictionOperationMap(evictionOperationMap);

        beanFactory.registerSingleton(CacheManager.INTERNAL_CACHE_MANAGER_BEAN_NAME, cacheManager);

        cacheManager.initialized();
        logger.debug(
                CacheManager.INTERNAL_CACHE_MANAGER_BEAN_NAME + " ========>>> " +
                        cacheManager.initializedInfo());

        InvocationInterceptor.init(cacheManager);
    }
}
