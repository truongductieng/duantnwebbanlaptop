package com.ductieng.model;

/**
 * Trạng thái yêu cầu trả hàng
 */
public enum ReturnStatus {
    REQUESTED("Đã gửi yêu cầu"), // Khách vừa tạo
    APPROVED("Đã phê duyệt"), // Admin chấp nhận
    REJECTED("Từ chối"), // Admin không chấp nhận
    ITEM_RECEIVED("Đã nhận hàng"), // Admin xác nhận nhận được hàng trả
    REFUNDED("Đã hoàn tiền"), // Hoàn tiền hoàn tất
    CANCELLED("Đã hủy bởi khách"); // Khách tự hủy

    private final String label;

    ReturnStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
