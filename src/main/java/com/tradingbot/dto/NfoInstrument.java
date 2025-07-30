package com.tradingbot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NfoInstrument {
    String instrument_token;
    String exchange_token;
    String tradingsymbol;
    String name;
    String last_price;
    String expiry;
    String strike;
    String tick_size;
    String lot_size;
    String instrument_type;
    String segment;
    String exchange;
}
