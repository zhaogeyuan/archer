package com.atpex.archer.spring.config;

import com.atpex.archer.roots.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.PriorityOrdered;

/**
 * Cache bean post processor
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public class CacheBeanPostProcessor implements BeanPostProcessor, PriorityOrdered {

    static final String CACHE_BEAN_POST_PROCESSOR_BEAN_NAME = "archer.cacheBeanPostProcessor";

    private static final Logger logger = LoggerFactory.getLogger(CacheBeanPostProcessor.class);

    @Override
    public Object postProcessBeforeInitialization(Object o, String s) throws BeansException {
        return o;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof Component) {
            ((Component) bean).initialized();
            logger.debug(
                    beanName + " ========>>> " +
                            ((Component) bean).initializedInfo()
            );
        }
        return bean;
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
