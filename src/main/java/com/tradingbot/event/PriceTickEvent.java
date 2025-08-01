package com.tradingbot.event;

import com.tradingbot.dto.PriceTick;
import org.springframework.context.ApplicationEvent;

/**
 * An application event that is published when a new price tick is received.
 * This helps in decoupling the tick producer from the tick consumers.
 */
public class PriceTickEvent extends ApplicationEvent {
    private final PriceTick priceTick;

    public PriceTickEvent(Object source, PriceTick priceTick) {
        super(source);
        this.priceTick = priceTick;
    }

    public PriceTick getPriceTick() {
        return priceTick;
    }
}