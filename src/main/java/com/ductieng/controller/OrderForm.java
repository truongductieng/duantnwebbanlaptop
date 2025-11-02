package com.ductieng.controller;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class OrderForm {
    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;

    @NotBlank(message = "Địa chỉ không được để trống")
    private String address;

    @NotBlank(message = "Số điện thoại không được để trống")
    private String phone;

    @Email(message = "Email không hợp lệ")
    @NotBlank(message = "Email không được để trống")
    private String email;

    // getters & setters
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
