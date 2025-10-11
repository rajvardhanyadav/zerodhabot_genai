package com.tradingbot.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for Kite API properties.
 * Maps properties with the prefix "kite.api" from the application configuration.
 */
@Configuration
@ConfigurationProperties(prefix = "kite.api")
@Getter
@Setter
public class KiteConfig {
    private String key;
    private String secret;
    private String baseUrl;
    private String loginUrl;
}