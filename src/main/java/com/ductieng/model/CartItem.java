package com.ductieng.model;

public class CartItem {

    private final Laptop laptop;
    private int quantity;

    public CartItem(Laptop laptop, int quantity) {
        this.laptop = laptop;
        this.quantity = quantity;
    }

    public Laptop getLaptop() {
        return laptop;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void increment() {
        this.quantity++;
    }
}
