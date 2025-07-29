package com.tradingbot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "trading.bot")
public class TradingBotProperties {
    private boolean enabled = true;
    private ZerodhaConfig zerodha = new ZerodhaConfig();
    private StrategyConfig strategy = new StrategyConfig();

    @Data
    public static class ZerodhaConfig {
        private String apiKey;
        private String apiSecret;
        private String accessToken;
        private String baseUrl = "https://api.kite.trade";
    }

    @Data
    public static class StrategyConfig {
        private String symbol = "BANKNIFTY";
        private int exitProfitPoints = 30;
        private int exitLossPoints = 1;
        private long executionInterval = 300000; // 5 minutes
        private String tradingStartTime = "09:15";
        private String tradingEndTime = "15:30";
    }
}
