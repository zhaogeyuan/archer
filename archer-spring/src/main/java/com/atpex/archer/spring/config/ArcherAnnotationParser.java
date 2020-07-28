package com.atpex.archer.spring.config;

import com.atpex.archer.CacheManager;
import com.atpex.archer.constants.Serialization;
import com.atpex.archer.spring.aop.CacheAdvisor;
import com.atpex.archer.spring.aop.CacheMethodInterceptor;
import com.atpex.archer.util.ReflectionUtil;
import org.springframework.aop.config.AopNamespaceUtils;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.parsing.CompositeComponentDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

import java.util.List;

/**
 * Archer annotation parser
 *
 * @author atpexgo.wu
 * @since 1.0
 */
public class ArcherAnnotationParser implements BeanDefinitionParser {

    @Override
    public BeanDefinition parse(Element element, ParserContext parserContext) {
        doParse(element, parserContext);
        return null;
    }

    private synchronized void doParse(Element element, ParserContext parserContext) {
        String enableMetrics = element.getAttribute("enable-metrics");
        String serialization = element.getAttribute("serialization");

        CacheManager.Config.metricsEnabled = Boolean.valueOf(enableMetrics);
        CacheManager.Config.valueSerialization = Serialization.valueOf(serialization);

        String[] basePackages = StringUtils.tokenizeToStringArray(element.getAttribute("base-package"), ",; \t\n");
        ReflectionUtil.forPackage(basePackages);
        AopNamespaceUtils.registerAutoProxyCreatorIfNecessary(parserContext, element);
        if (!parserContext.getRegistry().containsBeanDefinition(CacheAdvisor.CACHE_ADVISOR_BEAN_NAME)) {
            Object eleSource = parserContext.extractSource(element);
            CompositeComponentDefinition compositeDef = new CompositeComponentDefinition(element.getTagName(), eleSource);

            // shards info
            Element shardListElement = DomUtils.getChildElementByTagName(element, "shard-list");
            if (shardListElement != null) {
                List<Element> shardElements = DomUtils.getChildElementsByTagName(shardListElement, "bean");
                for (Element shardElement : shardElements) {
                    BeanDefinition shardBeanDefinition = parserContext.getDelegate().parseBeanDefinitionElement(shardElement).getBeanDefinition();
                    String shardBeanName = parserContext.getReaderContext().registerWithGeneratedName(shardBeanDefinition);
                    parserContext.getRegistry().registerBeanDefinition(shardBeanName, shardBeanDefinition);
                    compositeDef.addNestedComponent(new BeanComponentDefinition(shardBeanDefinition, shardBeanName));
                }
            }

            RootBeanDefinition serviceCacheableCommonConfigDef = new RootBeanDefinition(ArcherCommonConfiguration.class);
            serviceCacheableCommonConfigDef.setSource(eleSource);
            serviceCacheableCommonConfigDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
            String serviceCacheableCommonConfigName = parserContext.getReaderContext().registerWithGeneratedName(serviceCacheableCommonConfigDef);
            parserContext.getRegistry().registerBeanDefinition(serviceCacheableCommonConfigName, serviceCacheableCommonConfigDef);

            RootBeanDefinition interceptorDef = new RootBeanDefinition(CacheMethodInterceptor.class);
            interceptorDef.setSource(eleSource);
            interceptorDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
            String interceptorName = parserContext.getReaderContext().registerWithGeneratedName(interceptorDef);

            RootBeanDefinition advisorDef = new RootBeanDefinition(CacheAdvisor.class);
            advisorDef.setSource(eleSource);
            advisorDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
            advisorDef.getPropertyValues().addPropertyValue(new PropertyValue("adviceBeanName", interceptorName));
            advisorDef.getPropertyValues().addPropertyValue(new PropertyValue("basePackages", basePackages));
            parserContext.getRegistry().registerBeanDefinition(CacheAdvisor.CACHE_ADVISOR_BEAN_NAME, advisorDef);


            compositeDef.addNestedComponent(new BeanComponentDefinition(interceptorDef, interceptorName));
            compositeDef.addNestedComponent(new BeanComponentDefinition(advisorDef, CacheAdvisor.CACHE_ADVISOR_BEAN_NAME));
            compositeDef.addNestedComponent(new BeanComponentDefinition(serviceCacheableCommonConfigDef, serviceCacheableCommonConfigName));
            parserContext.registerComponent(compositeDef);

        }
    }

}
