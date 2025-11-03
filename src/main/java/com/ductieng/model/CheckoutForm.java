package com.ductieng.model;

import java.math.BigDecimal;

public class CheckoutForm {
    private String fullName;
    private String email;
    private String address;
    private String phone;
    private String paymentMethod;        // COD | VNPAY

    // Nhận từ form (hidden inputs)
    private String discountCode;         // VD: OFF10
    private Integer discountPercent;     // VD: 10
    private BigDecimal discountAmount;   // Số tiền giảm (đ)
    private BigDecimal totalAfterDiscount; // Tổng sau giảm (đ)

    // getters & setters
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getDiscountCode() { return discountCode; }
    public void setDiscountCode(String discountCode) { this.discountCode = discountCode; }
    public Integer getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(Integer discountPercent) { this.discountPercent = discountPercent; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public BigDecimal getTotalAfterDiscount() { return totalAfterDiscount; }
    public void setTotalAfterDiscount(BigDecimal totalAfterDiscount) { this.totalAfterDiscount = totalAfterDiscount; }
}
