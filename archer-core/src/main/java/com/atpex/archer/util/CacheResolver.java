package com.atpex.archer.util;

import com.atpex.archer.annotation.Cache;
import com.atpex.archer.annotation.CacheList;
import com.atpex.archer.annotation.CacheMulti;
import com.atpex.archer.annotation.Cacheable;
import com.atpex.archer.constants.Constants;
import com.atpex.archer.metadata.AbstractCacheMetadata;
import com.himalaya.service.cacheable.annotation.*;
import com.himalaya.service.cacheable.annotation.extra.MapTo;
import com.himalaya.service.cacheable.cache.metadata.impl.CacheEvictionMetadata;
import com.himalaya.service.cacheable.cache.metadata.impl.ListableCacheMetadata;
import com.himalaya.service.cacheable.cache.metadata.impl.ObjectCacheMetadata;
import com.himalaya.service.cacheable.context.CacheInvocationContext;
import com.himalaya.service.cacheable.exceptions.CacheBeanParsingException;
import com.himalaya.service.cacheable.exceptions.CacheOperationException;
import com.himalaya.service.cacheable.loader.MultipleLoader;
import com.himalaya.service.cacheable.loader.SingleLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Cache resolver
 *
 * @author atpexgo.wu
 * @since 1.0.0
 */
public class CacheResolver {

    private static final Logger logger = LoggerFactory.getLogger(CacheResolver.class);


    public static AbstractCacheMetadata resolveMetadata(Method method, Annotation annotation) {
        Class<?> methodClass = method.getDeclaringClass();
        Cacheable cacheable = ReflectionUtil.getCacheAnnotation(methodClass, Cacheable.class);
        return resolveMetadata(cacheable, method, annotation);
    }

    public static AbstractCacheMetadata resolveMetadata(Cacheable serviceCacheable, Method method, Annotation annotation) {
        String cacheArea = Constants.DEFAULT_CACHE_AREA;
        String keyPrefix = "";
        String valueSerializer = "";
        String keyGenerator = "";
        if (serviceCacheable != null) {
            cacheArea = serviceCacheable.cacheArea();
            keyPrefix = CommonUtils.isNotEmpty(serviceCacheable.prefix()) ? serviceCacheable.prefix() : serviceCacheable.value();
            valueSerializer = serviceCacheable.valueSerializer();
            keyGenerator = serviceCacheable.keyGenerator();
        }
        if (annotation instanceof Cache || annotation instanceof CacheMulti) {
            return resolveObjectCacheMetadata(method, annotation, cacheArea, keyPrefix, keyGenerator, valueSerializer, annotation);
        } else if (annotation instanceof CacheList) {
            ListCacheable listCacheable = (ListCacheable) annotation;
            return resolveListableCacheMetadata(method, annotation, cacheArea, keyPrefix, keyGenerator, valueSerializer, listCacheable);
        } else if (annotation instanceof CacheEvict) {
            CacheEvict cacheEvict = (CacheEvict) annotation;
            return resolveCacheEvictMetadata(method, annotation, cacheArea, keyPrefix, keyGenerator, cacheEvict);
        } else {
            throw new CacheBeanParsingException("Unsupported cache annotation : " + annotation);
        }
    }


    private
    static ObjectCacheMetadata
    resolveObjectCacheMetadata(Method method, Annotation cacheAnnotation, String cacheArea, String keyPrefix, String keyGenerator, String valueSerializer, Annotation annotation) {
        ObjectCacheMetadata metadata = new ObjectCacheMetadata();

        String key, condition, area, aKeyGenerator, aValueSerializer;
        boolean overwrite, penetrationProtect;
        long expiration, penetrationProtectTimeout;
        TimeUnit expirationTimeUnit, penetrationProtectTimeoutTimeUnit;
        if (annotation instanceof Cacheable) {
            Cacheable cacheable = (Cacheable) annotation;
            key = cacheable.key();
            condition = cacheable.condition();
            area = cacheable.cacheArea();
            aKeyGenerator = cacheable.keyGenerator();
            aValueSerializer = cacheable.valueSerializer();
            overwrite = cacheable.overwrite();
            penetrationProtect = cacheable.penetrationProtect();
            expiration = cacheable.expiration();
            penetrationProtectTimeout = cacheable.penetrationProtectTimeout();
            expirationTimeUnit = cacheable.expirationTimeUnit();
            penetrationProtectTimeoutTimeUnit = cacheable.penetrationProtectTimeoutTimeUnit();
        } else {
            MultipleCacheable cacheable = (MultipleCacheable) annotation;
            key = cacheable.elementKey();
            condition = cacheable.condition();
            area = cacheable.cacheArea();
            aKeyGenerator = cacheable.keyGenerator();
            aValueSerializer = cacheable.valueSerializer();
            overwrite = cacheable.overwrite();
            penetrationProtect = cacheable.penetrationProtect();
            expiration = cacheable.expiration();
            penetrationProtectTimeout = cacheable.penetrationProtectTimeout();
            expirationTimeUnit = cacheable.expirationTimeUnit();
            penetrationProtectTimeoutTimeUnit = cacheable.penetrationProtectTimeoutTimeUnit();

            metadata.setOrderBy(cacheable.orderBy());
            metadata.setMultiple(true);
        }
        resolveCommonMetadata(
                metadata,
                method,
                cacheAnnotation,
                resolveValue(cacheArea, area),
                keyPrefix,
                key,
                condition,
                resolveValue(keyGenerator, aKeyGenerator)
        );

        metadata.setInvokeAnyway(overwrite);
        metadata.setExpirationInMillis(expirationTimeUnit.toMillis(expiration));
        metadata.setPenetrationProtect(penetrationProtect);
        metadata.setPenetrationProtectTimeoutInMillis(penetrationProtectTimeoutTimeUnit.toMillis(penetrationProtectTimeout));
        metadata.setValueSerializer(resolveValue(valueSerializer, aValueSerializer));
        return metadata;
    }

    private
    static ListableCacheMetadata
    resolveListableCacheMetadata(Method method, Annotation cacheAnnotation, String cacheArea, String keyPrefix, String keyGenerator, String valueSerializer, ListCacheable listCacheable) {
        ListableCacheMetadata metadata = new ListableCacheMetadata();
        resolveCommonMetadata(
                metadata,
                method,
                cacheAnnotation,
                resolveValue(cacheArea, listCacheable.cacheArea()),
                keyPrefix,
                listCacheable.key(),
                listCacheable.condition(),
                resolveValue(keyGenerator, listCacheable.keyGenerator())
        );
        metadata.setElementKey(listCacheable.elementKey());
        metadata.setElementKeyGenerator(listCacheable.elementKeyGenerator());
        metadata.setInvokeAnyway(listCacheable.overwrite());
        metadata.setElementValueSerializer(resolveValue(valueSerializer, listCacheable.elementValueSerializer()));
        metadata.setExpirationInMillis(listCacheable.expirationTimeUnit().toMillis(listCacheable.expiration()));
        metadata.setPenetrationProtect(listCacheable.penetrationProtect());
        metadata.setPenetrationProtectTimeoutInMillis(listCacheable.penetrationProtectTimeoutTimeUnit().toMillis(listCacheable.penetrationProtectTimeout()));
        return metadata;
    }

    private
    static CacheEvictionMetadata
    resolveCacheEvictMetadata(Method method, Annotation cacheAnnotation, String cacheArea, String keyPrefix, String keyGenerator, CacheEvict cacheEvict) {
        CacheEvictionMetadata metadata = new CacheEvictionMetadata();
        resolveCommonMetadata(
                metadata,
                method,
                cacheAnnotation,
                resolveValue(cacheArea, cacheEvict.cacheArea()),
                keyPrefix,
                cacheEvict.key(),
                cacheEvict.condition(),
                resolveValue(keyGenerator, cacheEvict.keyGenerator())
        );
        metadata.setAfterInvocation(cacheEvict.afterInvocation());
        return metadata;
    }

    private
    static void
    resolveCommonMetadata(AbstractCacheMetadata metadata, Method method, Annotation cacheAnnotation, String area, String keyPrefix, String key, String condition, String keyGenerator) {
        metadata.setCacheAnnotation(cacheAnnotation);
        metadata.setArea(area);
        metadata.setKeyPrefix(keyPrefix);
        metadata.setKey(key);
        metadata.setCondition(condition);
        metadata.setKeyGenerator(keyGenerator);
        metadata.setMethodSignature(ReflectionUtil.getSignature(method));
    }

    private static String resolveValue(String global, String instant) {
        return CommonUtils.isEmpty(instant) ? global : instant;
    }

    public static Type parseCacheEntityType(Method method) {
        Type type = ReflectionUtil.getComponentOrArgumentType(method);
        if (type == null) {
            throw new RuntimeException("Listable/Multiple cache class (return type) should be collection or array");
        }
        return type;
    }

    public static SingleLoader<?> createListableCacheLoader() {
        InvocationHandler invocationHandler = (proxy, method1, args) -> {
            Object o = ((CacheInvocationContext) args[0]).getMethodInvoker().get();
            if (o == null) {
                return null;
            }
            return ReflectionUtil.transToList(o);
        };
        Object object = Proxy.newProxyInstance(SingleLoader.class.getClassLoader(), new Class[]{SingleLoader.class}, invocationHandler);
        return (SingleLoader<?>) object;
    }

    public static SingleLoader<?> createObjectCacheSingleLoader(final Method method) {
        InvocationHandler invocationHandler = (proxy, method1, args) -> {
            if (((CacheInvocationContext) args[0]).getMethodInvoker() == null) {
                method.setAccessible(true);
                return method.invoke(((CacheInvocationContext) args[0]).getTarget(), ((CacheInvocationContext) args[0]).getArgs());
            }
            return ((CacheInvocationContext) args[0]).getMethodInvoker().get();
        };
        Object object = Proxy.newProxyInstance(SingleLoader.class.getClassLoader(), new Class[]{SingleLoader.class}, invocationHandler);
        return (SingleLoader<?>) object;
    }

    @SuppressWarnings("rawtypes")
    public static MultipleLoader<?> createObjectCacheMultiLoader(final Method method) {
        InvocationHandler invocationHandler = (proxy, method1, args) -> {
            List<CacheInvocationContext> contexts = (List<CacheInvocationContext>) args[0];
            // should not be null!!
            Object target = contexts.get(0).getTarget();

            // the arguments here for example may be [(1,"s"),(2,"s")]
            List<Object[]> flattenedArgs = new ArrayList<>();
            for (CacheInvocationContext context : contexts) {
                flattenedArgs.add(context.getArgs());
            }

            // the roughened arguments here for example is parsed to [([1,2],"s")]
            Object[] roughenedArgs = ReflectionUtil.roughenArgs(method, flattenedArgs);
            method.setAccessible(true);
            Object result = method.invoke(target, roughenedArgs);

            // check if result size is the same with argument size
            List resultList = (List) ReflectionUtil.transToList(result);

            Map<CacheInvocationContext, Object> map = new HashMap<>();
            if (CollectionUtils.isEmpty(resultList)) {
                return map;
            }

            // check if @MapTo is declared and then resolve it
            Map<Integer, Annotation> indexedMapTo = ReflectionUtil.getIndexedMethodParameterCacheableAnnotations(method);
            if (CommonUtils.isEmpty(indexedMapTo)) {
                throw new CacheOperationException("The parameter of method declaring @MultipleCacheable should declare @MapTo.");
            }

            // gather all arguments declaring @MapTo to MappedArguments, and map it to context
            Map<MappedArguments, CacheInvocationContext> contextMap = new HashMap<>();
            List<Map.Entry<Integer, Annotation>> indexedMapToEntries = new ArrayList<>(indexedMapTo.entrySet());
            for (int i = 0; i < flattenedArgs.size(); i++) {
                Object[] flattenedArg = flattenedArgs.get(i);
                Object[] mappedArgs = new Object[indexedMapTo.size()];

                for (int i1 = 0; i1 < indexedMapToEntries.size(); i1++) {
                    int offset = indexedMapToEntries.get(i1).getKey();
                    mappedArgs[i1] = flattenedArg[offset];
                }
                // contexts size is the same with flattened arguments
                contextMap.put(new MappedArguments(mappedArgs), contexts.get(i));
            }

            for (Object element : resultList) {
                if (element == null) {
                    continue;
                }
                Object[] mappedArgs = new Object[indexedMapTo.size()];
                for (int i1 = 0; i1 < indexedMapToEntries.size(); i1++) {
                    MapTo mapTo = (MapTo) indexedMapToEntries.get(i1).getValue();
                    Object arg = new SpringElUtil.SpringELEvaluationContext(mapTo.value()).addVar("result$each", element).getValue();
                    mappedArgs[i1] = arg;
                }

                if (!contextMap.containsKey(new MappedArguments(mappedArgs))) {
                    // noise data
                    map.put(null, new Object());
                }

                map.put(contextMap.get(new MappedArguments(mappedArgs)), element);
            }

            return map;
        };
        Object object = Proxy.newProxyInstance(MultipleLoader.class.getClassLoader(), new Class[]{MultipleLoader.class}, invocationHandler);
        return (MultipleLoader<?>) object;
    }


    static class MappedArguments {
        private final Object[] objects;

        public MappedArguments(Object[] objects) {
            this.objects = objects;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof MappedArguments)) {
                return false;
            }
            MappedArguments that = (MappedArguments) o;
            return Arrays.equals(objects, that.objects);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(objects);
        }
    }

    public static SingleLoader<?> resolveSingleLoader(Method declaredMethod) {
        return createObjectCacheSingleLoader(declaredMethod);
    }

    public static MultipleLoader<?> resolveMultiLoader(Method declaredMethod) {
        // return type should be array or collection
        if (!ReflectionUtil.isCollectionOrArray(declaredMethod.getReturnType())) {
            throw new CacheBeanParsingException("The return type of method declaring @MultipleCacheable should be an array or collection!");
        }

        return CacheResolver.createObjectCacheMultiLoader(declaredMethod);
    }

}
