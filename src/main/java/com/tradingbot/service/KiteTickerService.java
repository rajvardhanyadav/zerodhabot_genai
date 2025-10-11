package com.tradingbot.service;

     import com.tradingbot.dto.PriceTick;
     import com.tradingbot.event.PriceTickEvent;
     import com.zerodhatech.models.Tick;
     import com.zerodhatech.ticker.KiteTicker;
     import jakarta.annotation.PreDestroy;
     import lombok.extern.slf4j.Slf4j;
     import org.springframework.beans.factory.annotation.Autowired;
     import org.springframework.context.ApplicationEventPublisher;
     import org.springframework.stereotype.Service;

     import java.util.*;
     import java.util.concurrent.ConcurrentHashMap;
     import java.util.concurrent.atomic.AtomicBoolean;

     /**
      * Service for managing the Kite Ticker WebSocket connection and subscriptions.
      */
     @Service
     @Slf4j
     public class KiteTickerService {

         private static final String CONNECTION_ALREADY_ACTIVE = "WebSocket is already connected or connection parameters are missing.";
         private static final String INITIALIZING_CONNECTION = "Initializing Kite Ticker WebSocket connection...";
         private static final String WEBSOCKET_CONNECTED = "WebSocket connected successfully.";
         private static final String WEBSOCKET_DISCONNECTED = "WebSocket disconnected.";
         private static final String DISCONNECTING_WEBSOCKET = "Disconnecting Kite Ticker WebSocket...";
         private static final String SUBSCRIBING_TOKENS = "Subscribing to new tokens on active connection.";
         private static final String UNSUBSCRIBING_TOKENS = "Unsubscribing from tokens on active connection.";

         private KiteTicker kiteTicker;
         private final Set<Long> subscribedTokens = ConcurrentHashMap.newKeySet();
         private final AtomicBoolean isConnected = new AtomicBoolean(false);

         @Autowired
         private ApplicationEventPublisher eventPublisher;

         /**
          * Connects to the Kite Ticker WebSocket.
          * This method is idempotent and safe to call multiple times.
          */
         public synchronized void connect(String apiKey, String accessToken) {
             if (isConnected.get() || apiKey == null || accessToken == null) {
                 log.info(CONNECTION_ALREADY_ACTIVE);
                 return;
             }

             log.info(INITIALIZING_CONNECTION);
             kiteTicker = new KiteTicker(accessToken, apiKey);

             kiteTicker.setOnConnectedListener(() -> handleConnectionEstablished());
             kiteTicker.setOnDisconnectedListener(() -> handleDisconnection());
             kiteTicker.setOnTickerArrivalListener(this::processTicks);

             kiteTicker.setTryReconnection(true);
             kiteTicker.connect();
         }

         private void handleConnectionEstablished() {
             log.info(WEBSOCKET_CONNECTED);
             isConnected.set(true);
             if (!subscribedTokens.isEmpty()) {
                 ArrayList<Long> tokensToSubscribe = new ArrayList<>(subscribedTokens);
                 log.info("Re-subscribing to {} tokens.", tokensToSubscribe.size());
                 kiteTicker.subscribe(tokensToSubscribe);
                 kiteTicker.setMode(tokensToSubscribe, KiteTicker.modeFull);
             }
         }

         private void handleDisconnection() {
             log.info(WEBSOCKET_DISCONNECTED);
             isConnected.set(false);
         }

         void processTicks(List<Tick> ticks) {
             if (ticks.isEmpty()) {
                 return;
             }
             log.info("processTicks() called with {} ticks", ticks.size());
             for (Tick tick : ticks) {
                 log.info(tick.getInstrumentToken()+" "+tick.getLastTradedPrice());
                 PriceTick priceTick = new PriceTick(tick.getInstrumentToken(), tick.getLastTradedPrice());
                 eventPublisher.publishEvent(new PriceTickEvent(this, priceTick));
             }
         }

         /**
          * Subscribes to a list of instrument tokens.
          * If the WebSocket is not connected, it stores the tokens and subscribes upon connection.
          * @param tokens A list of instrument tokens to subscribe to.
          */
         public void subscribe(List<Long> tokens) {
             Optional.ofNullable(tokens).filter(t -> !t.isEmpty()).ifPresent(validTokens -> {
                 log.info("Request to subscribe to tokens: {}", validTokens);
                 subscribedTokens.addAll(validTokens);

                 if (isConnected.get() && kiteTicker != null) {
                     log.info(SUBSCRIBING_TOKENS);
                     ArrayList<Long> tokensToSubscribe = new ArrayList<>(validTokens);
                     kiteTicker.subscribe(tokensToSubscribe);
                     kiteTicker.setMode(tokensToSubscribe, KiteTicker.modeFull);
                 }
             });
         }

         /**
          * Unsubscribes from a list of instrument tokens.
          * @param tokens A list of instrument tokens to unsubscribe from.
          */
         public void unsubscribe(List<Long> tokens) {
             Optional.ofNullable(tokens).filter(t -> !t.isEmpty()).ifPresent(validTokens -> {
                 log.info("Request to unsubscribe from tokens: {}", validTokens);
                 subscribedTokens.removeAll(validTokens);

                 if (isConnected.get() && kiteTicker != null) {
                     log.info(UNSUBSCRIBING_TOKENS);
                     kiteTicker.unsubscribe(new ArrayList<>(validTokens));
                 }
             });
         }

         /**
          * Disconnects the WebSocket connection gracefully.
          */
         @PreDestroy
         public synchronized void disconnect() {
             if (isConnected.get() && kiteTicker != null) {
                 log.info(DISCONNECTING_WEBSOCKET);
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