package com.bigkhoa.service;

import com.bigkhoa.model.CartItem;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface CartService {

    void add(Long laptopId);

    void decrement(Long id);

    void remove(Long id);

    List<CartItem> getItems();

    int getItemCount();

    double getTotalPrice();

    void clear();

    // Đồng bộ với CartServiceImpl anh gửi
    void addToCart(Long id, int quantity, Authentication auth);
}
