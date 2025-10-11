package com.tradingbot.config;

    import lombok.Getter;
    import lombok.Setter;
    import org.springframework.boot.context.properties.ConfigurationProperties;
    import org.springframework.context.annotation.Configuration;

    /**
     * Configuration class for Trading Bot properties.
     * Maps properties with the prefix "trading.bot" from the application configuration.
     */
    @Getter
    @Setter
    @Configuration
    @ConfigurationProperties(prefix = "trading.bot")
    public class TradingBotProperties {
        private boolean enabled = true;
        private ZerodhaConfig zerodha = new ZerodhaConfig();
        private StrategyConfig strategy = new StrategyConfig();

        /**
         * Configuration for Zerodha API.
         */
        @Getter
        @Setter
        public static class ZerodhaConfig {
            private String apiKey;
            private String apiSecret;
            private String accessToken;
            private String baseUrl = "https://api.kite.trade";
        }

        /**
         * Configuration for trading strategies.
         */
        @Getter
        @Setter
        public static class StrategyConfig {
            private String symbol = "BANKNIFTY";
            private int exitProfitPoints = 30;
            private int exitLossPoints = 1;
            private long executionInterval = 300000; // 5 minutes
            private String tradingStartTime = "09:15";
            private String tradingEndTime = "15:30";
        }
    }