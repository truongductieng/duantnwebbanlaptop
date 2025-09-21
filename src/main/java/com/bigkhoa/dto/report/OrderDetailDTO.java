package com.bigkhoa.dto.report;

import java.math.BigDecimal;
import java.util.List;

public record OrderDetailDTO(
        Long id,
        String code,
        String customer,          // Tên khách (fullName/name/username/email)
        String customerPhone,     // SĐT khách
        String shippingAddress,   // ĐỊA CHỈ GIAO HÀNG (mới)
        BigDecimal total,
        String discountCode,
        BigDecimal discountAmount,
        BigDecimal discountPercent,
        List<ItemDetailDTO> items
) {}
