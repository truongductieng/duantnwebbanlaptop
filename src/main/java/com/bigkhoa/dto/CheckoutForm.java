package com.bigkhoa.dto;

import java.math.BigDecimal;

public class CheckoutForm {
    private String fullName;
    private String email;
    private String address;
    private String phone;

    /** Mã giảm giá áp dụng (nếu có) */
    private String discountCode;

    /** Phần trăm giảm (VD: 10 = 10%) */
    private Integer discountPercent = 0;

    /** Số tiền đã giảm (đ) */
    private BigDecimal discountAmount = BigDecimal.ZERO;

    /** Tổng sau giảm (đ) – dùng để hiển thị; service vẫn tự tính lại để tránh fake form */
    private BigDecimal totalAfterDiscount = BigDecimal.ZERO;

    /** COD hoặc VNPAY */
    private String paymentMethod;

    // --- Getters & Setters ---

    public String getFullName() {
        return fullName;
    }
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }
    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }
    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getDiscountCode() {
        return discountCode;
    }
    public void setDiscountCode(String discountCode) {
        this.discountCode = discountCode;
    }

    public Integer getDiscountPercent() {
        return discountPercent;
    }
    public void setDiscountPercent(Integer discountPercent) {
        this.discountPercent = discountPercent;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }
    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getTotalAfterDiscount() {
        return totalAfterDiscount;
    }
    public void setTotalAfterDiscount(BigDecimal totalAfterDiscount) {
        this.totalAfterDiscount = totalAfterDiscount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }
    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}
