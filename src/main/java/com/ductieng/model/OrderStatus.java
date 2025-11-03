package com.ductieng.model;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum OrderStatus {
    PENDING("Đang chờ"),
    CONFIRMED("Đã xác nhận"),
    SHIPPED("Đang giao hàng"),
    DELIVERED("Đã giao hàng"),
    CANCELED("Đã hủy"),

    @Deprecated
    COMPLETED("Hoàn tất");

    private final String label;
    OrderStatus(String label) { this.label = label; }
    public String getLabel() { return label; }

    /** Danh sách trạng thái hiển thị cho Admin (không có COMPLETED) */
    public static List<OrderStatus> adminOptions() {
        return Arrays.stream(values())
                .filter(s -> s != COMPLETED)
                .collect(Collectors.toList());
    }
}
