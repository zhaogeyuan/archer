package com.atpex.archer.spring.config;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * Cache namespace handler
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public class CacheNamespaceHandler extends NamespaceHandlerSupport {

    @Override
    public void init() {
        registerBeanDefinitionParser("enable", new ArcherAnnotationParser());
    }
}
