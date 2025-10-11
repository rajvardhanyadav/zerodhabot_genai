package com.tradingbot.dto;

    import lombok.Getter;
    import lombok.NoArgsConstructor;
    import lombok.Setter;

    import java.math.BigDecimal;

    /**
     * DTO representing account information for a user.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    public final class AccountInfo {
        private String userId;
        private String userName;
        private BigDecimal availableMargin;
        private BigDecimal utilizedMargin;
        private BigDecimal totalMargin;
        private BigDecimal dayPnl;
    }