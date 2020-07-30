package com.github.attt.archer.spring.autoconfigure;


import com.github.attt.archer.spring.aop.CacheAdvisor;
import com.github.attt.archer.spring.aop.CacheMethodInterceptor;
import com.github.attt.archer.spring.autoconfigure.annotation.EnableArcher;
import com.github.attt.archer.util.ReflectionUtil;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.context.annotation.Role;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Archer proxy config
 *
 * @author atpexgo.wu
 * @since 1.0
 */
@Configuration
public class ArcherProxyConfiguration implements ImportAware {

    private AnnotationAttributes enableMethodCache;

    @Override
    public void setImportMetadata(AnnotationMetadata importMetadata) {
        this.enableMethodCache = AnnotationAttributes.fromMap(
                importMetadata.getAnnotationAttributes(EnableArcher.class.getName(), false));
        if (this.enableMethodCache == null) {
            throw new IllegalArgumentException(
                    "@EnableArcher is not present on importing class " + importMetadata.getClassName());
        }
    }


    @ConditionalOnMissingBean(name = CacheAdvisor.CACHE_ADVISOR_BEAN_NAME)
    @Bean(name = CacheAdvisor.CACHE_ADVISOR_BEAN_NAME)
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public CacheAdvisor cacheAdvisor() {
        CacheAdvisor advisor = new CacheAdvisor();
        advisor.setAdviceBeanName(CacheAdvisor.CACHE_ADVISOR_BEAN_NAME);
        advisor.setAdvice(this.cacheMethodInterceptor());
        if (this.enableMethodCache != null) {
            String[] basePackages = this.enableMethodCache.getStringArray("basePackages");
            advisor.setBasePackages(basePackages);
            ReflectionUtil.forPackage(basePackages);
            advisor.setOrder(this.enableMethodCache.<Integer>getNumber("order"));
        }
        return advisor;
    }

    @ConditionalOnMissingBean(name = CacheMethodInterceptor.CACHE_METHOD_INTERCEPTOR_BEAN_NAME)
    @Bean(name = CacheMethodInterceptor.CACHE_METHOD_INTERCEPTOR_BEAN_NAME)
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public CacheMethodInterceptor cacheMethodInterceptor() {
        return new CacheMethodInterceptor();
    }

}