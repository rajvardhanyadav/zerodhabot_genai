package com.tradingbot.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Instrument {
    private String tradingSymbol;
    private String name;
    private double lastPrice;
    private String segment;
    private String exchange;
    private int instrumentToken;
    private int lotSize;
    private String instrumentType;
    private double strike;
    private String expiry;
}