package com.tradingbot.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;

/**
 * Model representing the response of an order in the trading system.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderResponse {
    /** The unique identifier of the order. */
    private String orderId;

    /** The status of the order (e.g., SUCCESS, FAILED). */
    private OrderStatus status;

    /** The message associated with the order response. */
    private String message;

    /** Enum representing possible order statuses. */
    public enum OrderStatus {
        SUCCESS,
        FAILED,
        PENDING
    }
}