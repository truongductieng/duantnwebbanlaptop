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
@SessionScope // Giữ giỏ hàng riêng cho mỗi session
public class CartServiceImpl implements CartService {

    private final LaptopRepository laptopRepo;
    private final List<CartItem> items = new ArrayList<>();

    @Autowired
    public CartServiceImpl(LaptopRepository laptopRepo) {
        this.laptopRepo = laptopRepo;
    }

    @Override
    public void add(Long laptopId) {
        Laptop lp = laptopRepo.findById(laptopId)
                .orElseThrow(() -> new IllegalArgumentException("Laptop không tồn tại"));

        // KIỂM TRA SỐ LƯỢNG TỒN KHO
        Integer availableQty = lp.getQuantity();
        if (availableQty == null || availableQty < 1) {
            throw new IllegalStateException("Sản phẩm '" + lp.getName() + "' đã hết hàng!");
        }

        for (CartItem ci : items) {
            if (ci.getLaptop().getId().equals(laptopId)) {
                int newQty = ci.getQuantity() + 1;
                if (newQty > availableQty) {
                    throw new IllegalStateException(
                            String.format("Không đủ hàng! Tồn kho: %d, trong giỏ: %d",
                                    availableQty, ci.getQuantity()));
                }
                ci.increment(); // +1
                return;
            }
        }
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
        if (quantity < 1)
            quantity = 1;

        Laptop lp = laptopRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Laptop không tồn tại"));

        // KIỂM TRA SỐ LƯỢNG TỒN KHO
        Integer availableQty = lp.getQuantity();
        if (availableQty == null || availableQty < 1) {
            throw new IllegalStateException("Sản phẩm '" + lp.getName() + "' đã hết hàng!");
        }

        // Kiểm tra nếu đã có trong giỏ
        for (CartItem ci : items) {
            if (ci.getLaptop().getId().equals(id)) {
                int newQty = ci.getQuantity() + quantity;
                if (newQty > availableQty) {
                    throw new IllegalStateException(
                            String.format("Không đủ hàng! Tồn kho: %d, trong giỏ: %d, yêu cầu thêm: %d",
                                    availableQty, ci.getQuantity(), quantity));
                }
                ci.setQuantity(newQty);
                return;
            }
        }

        // Thêm mới vào giỏ
        if (quantity > availableQty) {
            throw new IllegalStateException(
                    String.format("Không đủ hàng! Tồn kho: %d, yêu cầu: %d", availableQty, quantity));
        }
        items.add(new CartItem(lp, quantity));
    }
}
