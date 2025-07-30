package com.tradingbot.service;

import com.tradingbot.dto.PriceTick;
import com.tradingbot.event.PriceTickEvent; // Import the new event
import com.zerodhatech.models.Tick;
import com.zerodhatech.ticker.KiteTicker;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher; // Import publisher
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
public class KiteTickerService {

    private KiteTicker kiteTicker;
    private final Set<Long> subscribedTokens = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean isConnected = new AtomicBoolean(false);

    // Inject the ApplicationEventPublisher instead of PriceUpdateProcessor
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    /**
     * Connects to the Kite Ticker WebSocket.
     * This method is idempotent and safe to call multiple times.
     */
    public synchronized void connect(String apiKey, String accessToken) {
        if (isConnected.get() || apiKey == null || accessToken == null) {
            log.info("WebSocket is already connected or connection parameters are missing.");
            return;
        }

        log.info("Initializing Kite Ticker WebSocket connection...");
        kiteTicker = new KiteTicker(accessToken, apiKey);

        kiteTicker.setOnConnectedListener(() -> {
            log.info("WebSocket connected successfully.");
            isConnected.set(true);
            if (!subscribedTokens.isEmpty()) {
                ArrayList<Long> tokensToSubscribe = new ArrayList<>(subscribedTokens);
                log.info("Re-subscribing to {} tokens.", tokensToSubscribe.size());
                kiteTicker.subscribe(tokensToSubscribe);
                kiteTicker.setMode(tokensToSubscribe, KiteTicker.modeFull);
            }
        });

        kiteTicker.setOnDisconnectedListener(() -> {
            log.info("WebSocket disconnected.");
            isConnected.set(false);
        });

        kiteTicker.setOnTickerArrivalListener(ticks -> {
            if (ticks.isEmpty()) {
                return;
            }
//            log.info("Received {} ticks.", ticks.size());
            for (Tick tick : ticks) {
                // Convert to our internal DTO
                PriceTick priceTick = new PriceTick(tick.getInstrumentToken(), tick.getLastTradedPrice());
                // Publish an event for other parts of the application to consume
                eventPublisher.publishEvent(new PriceTickEvent(this, priceTick));
            }
        });

        kiteTicker.setTryReconnection(true);
        kiteTicker.connect();
    }

    /**
     * Subscribes to a list of instrument tokens.
     * If the WebSocket is not connected, it stores the tokens and subscribes upon connection.
     * @param tokens A list of instrument tokens to subscribe to.
     */
    public void subscribe(List<Long> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return;
        }
        log.info("Request to subscribe to tokens: {}", tokens);
        subscribedTokens.addAll(tokens);

        if (isConnected.get() && kiteTicker != null) {
            log.info("Subscribing to new tokens on active connection.");
            ArrayList<Long> tokensToSubscribe = new ArrayList<>(tokens);
            kiteTicker.subscribe(tokensToSubscribe);
            kiteTicker.setMode(tokensToSubscribe, KiteTicker.modeFull);
        }
    }

    /**
     * Unsubscribes from a list of instrument tokens.
     * @param tokens A list of instrument tokens to unsubscribe from.
     */
    public void unsubscribe(List<Long> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return;
        }
        log.info("Request to unsubscribe from tokens: {}", tokens);
        subscribedTokens.removeAll(tokens);

        if (isConnected.get() && kiteTicker != null) {
            log.info("Unsubscribing from tokens on active connection.");
            kiteTicker.unsubscribe(new ArrayList<>(tokens));
        }
    }

    /**
     * Disconnects the WebSocket connection gracefully.
     */
    @PreDestroy
    public synchronized void disconnect() {
        if (isConnected.get() && kiteTicker != null) {
            log.info("Disconnecting Kite Ticker WebSocket...");
            kiteTicker.disconnect();
            isConnected.set(false);
            subscribedTokens.clear();
            log.info("WebSocket disconnected and subscriptions cleared.");
        }
    }

    public boolean isConnected() {
        return isConnected.get();
    }
}