package com.bigkhoa.service;

public interface DiscountService {
    /**
     * Trả về phần trăm giảm (ví dụ 10 cho 10%), 
     * hoặc null nếu code không tồn tại/không hợp lệ.
     */
	Integer getDiscountPercent(String code);
}
