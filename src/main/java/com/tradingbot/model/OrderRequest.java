package com.tradingbot.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderRequest {
    private String variety = "regular";
    private String exchange = "NFO";
    private String tradingSymbol;
    private String transactionType;
    private int quantity;
    private String product = "MIS";
    private String orderType = "MARKET";
    private double price = 0.0;
    private String validity = "DAY";
}
