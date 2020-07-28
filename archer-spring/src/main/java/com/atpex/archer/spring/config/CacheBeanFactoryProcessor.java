package com.atpex.archer.spring.config;

import com.atpex.archer.CacheManager;
import com.atpex.archer.util.InfoPrinter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * Cache bean factory post processor
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public class CacheBeanFactoryProcessor implements BeanFactoryPostProcessor {

    static final String CACHE_BEAN_FACTORY_PROCESSOR_BEAN_NAME = "archer.cacheBeanFactoryProcessor";

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        CacheManager cacheManager = beanFactory.getBean(CacheManager.INTERNAL_CACHE_MANAGER_BEAN_NAME, CacheManager.class);
        InfoPrinter.printComponentUsageInfo(cacheManager);
    }
}
