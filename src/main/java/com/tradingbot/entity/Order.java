// java
package com.tradingbot.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import lombok.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_order_orderid", columnList = "orderId")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Kite external order id
    @Column(name = "order_id", nullable = false, unique = true)
    private String orderId;

    @Column(name = "placed_by")
    private String placedBy;

    @Column(name = "exchange_order_id")
    private String exchangeOrderId;

    @Column(name = "parent_order_id")
    private String parentOrderId;

    @Column(name = "status")
    private String status;

    @Column(name = "status_message")
    private String statusMessage;

    @Column(name = "status_message_raw", columnDefinition = "text")
    private String statusMessageRaw;

    @Column(name = "order_timestamp")
    private LocalDateTime orderTimestamp;

    @Column(name = "exchange_update_timestamp")
    private LocalDateTime exchangeUpdateTimestamp;

    @Column(name = "exchange_timestamp")
    private LocalDateTime exchangeTimestamp;

    @Column(name = "variety")
    private String variety;

    @Column(name = "modified")
    private Boolean modified;

    @Column(name = "exchange")
    private String exchange;

    @Column(name = "tradingsymbol")
    private String tradingSymbol;

    @Column(name = "instrument_token")
    private Long instrumentToken;

    @Column(name = "order_type")
    private String orderType;

    @Column(name = "transaction_type")
    private String transactionType;

    @Column(name = "validity")
    private String validity;

    @Column(name = "product")
    private String product;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "disclosed_quantity")
    private Integer disclosedQuantity;

    @Column(name = "price")
    private BigDecimal price;

    @Column(name = "trigger_price")
    private BigDecimal triggerPrice;

    @Column(name = "average_price")
    private BigDecimal averagePrice;

    @Column(name = "filled_quantity")
    private Integer filledQuantity;

    @Column(name = "pending_quantity")
    private Integer pendingQuantity;

    @Column(name = "cancelled_quantity")
    private Integer cancelledQuantity;

    @Column(name = "market_protection")
    private BigDecimal marketProtection;

    @Column(name = "tag")
    private String tag;

    @Column(name = "guid")
    private String guid;

    @Lob
    @Column(name = "meta", columnDefinition = "text")
    private String meta; // store meta JSON as string

    @Lob
    @Column(name = "raw_json", columnDefinition = "text")
    private String rawJson;
}