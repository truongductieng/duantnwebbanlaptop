package com.bigkhoa.dto;

import java.math.BigDecimal;

public class RevenueDataDto {
    private final String period;      // "2025-07-28" hoặc "2025-07"
    private final BigDecimal revenue;

    public RevenueDataDto(String period, BigDecimal revenue) {
        this.period = period;
        this.revenue = revenue;
    }
    public String getPeriod()     { return period; }
    public BigDecimal getRevenue(){ return revenue; }
}
