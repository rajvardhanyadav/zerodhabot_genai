// java
package com.tradingbot.entity;

import lombok.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "positions", indexes = {
        @Index(name = "idx_position_symbol", columnList = "tradingSymbol")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Position {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String tradingSymbol;
    private Integer quantity;
    private BigDecimal averagePrice;
    private BigDecimal lastPrice;
    private BigDecimal pnl;

    private LocalDateTime asOf;

    @Lob
    @Column(columnDefinition = "text")
    private String rawJson;
}