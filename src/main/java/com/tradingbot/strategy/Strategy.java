package com.tradingbot.strategy;

import com.tradingbot.dto.TradingConfigDto;
import org.springframework.stereotype.Service;

public interface Strategy {
    StrategyType getType();
    void execute(TradingConfigDto config);
}
