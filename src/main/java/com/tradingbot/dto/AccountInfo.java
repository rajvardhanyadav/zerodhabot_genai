package com.tradingbot.dto;

import java.math.BigDecimal;

public class AccountInfo {
    private String userId;
    private String userName;
    private BigDecimal availableMargin;
    private BigDecimal utilizedMargin;
    private BigDecimal totalMargin;
    private BigDecimal dayPnl;

    // Constructors
    public AccountInfo() {}

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public BigDecimal getAvailableMargin() { return availableMargin; }
    public void setAvailableMargin(BigDecimal availableMargin) { this.availableMargin = availableMargin; }
    public BigDecimal getUtilizedMargin() { return utilizedMargin; }
    public void setUtilizedMargin(BigDecimal utilizedMargin) { this.utilizedMargin = utilizedMargin; }
    public BigDecimal getTotalMargin() { return totalMargin; }
    public void setTotalMargin(BigDecimal totalMargin) { this.totalMargin = totalMargin; }
    public BigDecimal getDayPnl() { return dayPnl; }
    public void setDayPnl(BigDecimal dayPnl) { this.dayPnl = dayPnl; }
}