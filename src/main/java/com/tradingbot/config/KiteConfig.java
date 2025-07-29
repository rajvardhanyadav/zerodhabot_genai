package com.tradingbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "kite.api")
public class KiteConfig {
    private String key;
    private String secret;
    private String baseUrl;
    private String loginUrl;

    // Getters and Setters
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getLoginUrl() { return loginUrl; }
    public void setLoginUrl(String loginUrl) { this.loginUrl = loginUrl; }
}
