package com.atpex.archer.spring.autoconfigure;

import com.atpex.archer.spring.autoconfigure.properties.CacheProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author atpexgo.wu
 * @since 1.0
 */
@EnableConfigurationProperties(CacheProperties.class)
@Configuration
public class PropertiesAutoConfiguration {
}
