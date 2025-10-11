package com.tradingbot.dto;

    import lombok.AllArgsConstructor;
    import lombok.EqualsAndHashCode;
    import lombok.Getter;
    import lombok.NoArgsConstructor;
    import lombok.Setter;
    import lombok.ToString;

    /**
     * DTO representing an NFO (National Futures and Options) instrument.
     */
    @Getter
    @Setter
    @ToString
    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    public final class NfoInstrument {
        private String instrumentToken;
        private String exchangeToken;
        private String tradingSymbol;
        private String name;
        private String lastPrice;
        private String expiry;
        private String strike;
        private String tickSize;
        private String lotSize;
        private String instrumentType;
        private String segment;
        private String exchange;
    }