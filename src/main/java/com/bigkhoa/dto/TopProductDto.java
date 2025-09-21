package com.bigkhoa.dto;

import java.math.BigDecimal;

public class TopProductDto {
    private Long productId;
    private String productName;
    private Long quantity;
    private BigDecimal revenue;

    // Constructor khớp chính xác với JPQL: (Long, String, Long, BigDecimal)
    public TopProductDto(Long productId, String productName, Long quantity, BigDecimal revenue) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = (quantity == null ? 0L : quantity);
        this.revenue  = (revenue  == null ? BigDecimal.ZERO : revenue);
    }

    // Getters (đủ để Thymeleaf/jackson dùng)
    public Long getProductId()     { return productId; }
    public String getProductName() { return productName; }
    public Long getQuantity()      { return quantity; }
    public BigDecimal getRevenue() { return revenue; }
}
