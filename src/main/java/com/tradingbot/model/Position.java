package com.tradingbot.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Position {
    private String tradingSymbol;
    private String product;
    private int quantity;
    private double averagePrice;
    private double lastPrice;
    private double pnl;
    private String instrumentToken;
}
