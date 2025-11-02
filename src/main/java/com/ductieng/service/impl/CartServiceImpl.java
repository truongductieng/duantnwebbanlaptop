package com.ductieng.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

import com.ductieng.model.CartItem;
import com.ductieng.model.Laptop;
import com.ductieng.repository.LaptopRepository;
import com.ductieng.service.CartService;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Service
@SessionScope  // Giữ giỏ hàng riêng cho mỗi session
public class CartServiceImpl implements CartService {

    private final LaptopRepository laptopRepo;
    private final List<CartItem> items = new ArrayList<>();

    @Autowired
    public CartServiceImpl(LaptopRepository laptopRepo) {
        this.laptopRepo = laptopRepo;
    }

    @Override
    public void add(Long laptopId) {
        for (CartItem ci : items) {
            if (ci.getLaptop().getId().equals(laptopId)) {
                ci.increment();  // +1
                return;
            }
        }
        Laptop lp = laptopRepo.findById(laptopId)
                              .orElseThrow(() -> new IllegalArgumentException("Laptop không tồn tại"));
        items.add(new CartItem(lp, 1));
    }

    @Override
    public void decrement(Long id) {
        for (Iterator<CartItem> it = items.iterator(); it.hasNext();) {
            CartItem ci = it.next();
            if (ci.getLaptop().getId().equals(id)) {
                ci.setQuantity(ci.getQuantity() - 1);
                if (ci.getQuantity() <= 0) {
                    it.remove();
                }
                return;
            }
        }
    }

    @Override
    public void remove(Long id) {
        items.removeIf(ci -> ci.getLaptop().getId().equals(id));
    }

    @Override
    public List<CartItem> getItems() {
        return items;
    }

    @Override
    public int getItemCount() {
        return items.stream().mapToInt(CartItem::getQuantity).sum();
    }

    @Override
    public double getTotalPrice() {
        return items.stream()
                    .mapToDouble(ci -> ci.getLaptop().getPrice() * ci.getQuantity())
                    .sum();
    }

    @Override
    public void clear() {
        items.clear();
    }

    // ✅ Implement thêm để hỗ trợ /product/{id}/add-to-cart
    @Override
    public void addToCart(Long id, int quantity, Authentication auth) {
        if (quantity < 1) quantity = 1;
        for (CartItem ci : items) {
            if (ci.getLaptop().getId().equals(id)) {
                ci.setQuantity(ci.getQuantity() + quantity);
                return;
            }
        }
        Laptop lp = laptopRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Laptop không tồn tại"));
        items.add(new CartItem(lp, quantity));
    }
}
