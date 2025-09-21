package com.bigkhoa.dto.report;

import java.math.BigDecimal;

public record ItemDetailDTO(Long productId,
                            String productName,
                            int quantity,
                            BigDecimal unitPrice,
                            BigDecimal lineTotal) {}
