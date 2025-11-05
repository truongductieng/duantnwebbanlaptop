package com.ductieng.controller;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ductieng.model.CartItem;
import com.ductieng.service.CartService;
import com.ductieng.service.DiscountService;

@Controller
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;
    private final DiscountService discountService;

    public CartController(CartService cartService, DiscountService discountService) {
        this.cartService = cartService;
        this.discountService = discountService;
    }

    // Xem giỏ
    @GetMapping
    public String viewCart(Model model, HttpSession session) {
        List<CartItem> items = cartService.getItems();
        model.addAttribute("cartItems", items);
        model.addAttribute("totalCount", cartService.getItemCount());

        long totalPrice = items.stream()
                .mapToLong(i -> (long) (i.getLaptop().getPrice() * i.getQuantity()))
                .sum();
        model.addAttribute("totalPrice", totalPrice);

        // Thêm các thông tin giảm giá từ session (để hiển thị)
        model.addAttribute("discountCode", session.getAttribute("discountCode"));
        model.addAttribute("discountPercent", session.getAttribute("discountPercent")); // NEW
        model.addAttribute("discountAmount", session.getAttribute("discountAmount"));
        model.addAttribute("totalAfterDiscount", session.getAttribute("totalAfterDiscount"));
        return "cart";
    }

    // Thêm 1 sản phẩm
    @PostMapping("/add/{id}")
    public String addToCart(@PathVariable Long id, RedirectAttributes ra) {
        try {
            cartService.add(id);
            ra.addFlashAttribute("success", "Đã thêm sản phẩm vào giỏ hàng!");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cart";
    }

    // Giảm 1
    @PostMapping("/decrement/{id}")
    public String decrement(@PathVariable Long id) {
        cartService.decrement(id);
        return "redirect:/cart";
    }

    // Áp dụng mã giảm giá (POST)
    @PostMapping("/apply-discount")
    public String applyDiscount(
            @RequestParam(name = "discountCode", required = false) String discountCode,
            HttpSession session,
            RedirectAttributes ra) {
        // Validate input
        if (discountCode == null || discountCode.trim().isEmpty()) {
            clearDiscount(session);
            ra.addFlashAttribute("error", "Vui lòng nhập mã giảm giá.");
            return "redirect:/cart";
        }

        String code = discountCode.trim().toUpperCase();

        // Tổng tiền hiện tại
        long totalPrice = cartService.getItems().stream()
                .mapToLong(i -> (long) (i.getLaptop().getPrice() * i.getQuantity()))
                .sum();

        // Lấy phần trăm giảm
        Integer percent = discountService.getDiscountPercent(code);
        if (percent == null || percent <= 0) {
            clearDiscount(session);
            ra.addFlashAttribute("error", "Mã không hợp lệ hoặc đã hết hạn.");
            return "redirect:/cart";
        }

        long discountAmount = totalPrice * percent / 100;
        if (discountAmount > totalPrice)
            discountAmount = totalPrice;
        long totalAfter = totalPrice - discountAmount;

        // Lưu session cho checkout
        session.setAttribute("discountCode", code);
        session.setAttribute("discountPercent", percent); // NEW
        session.setAttribute("discountAmount", discountAmount);
        session.setAttribute("totalAfterDiscount", totalAfter);

        ra.addFlashAttribute("message", "Áp dụng mã giảm giá thành công!");
        return "redirect:/cart";
    }

    // Chặn GET /apply-discount (ai đó mở trực tiếp URL)
    @GetMapping("/apply-discount")
    public String applyDiscountGet(RedirectAttributes ra) {
        ra.addFlashAttribute("error", "Vui lòng nhập mã giảm giá trong giỏ hàng.");
        return "redirect:/cart";
    }

    // Xóa 1 mục
    @PostMapping("/remove/{id}")
    public String removeItem(@PathVariable Long id) {
        cartService.remove(id);
        return "redirect:/cart";
    }

    // Xóa hết
    @PostMapping("/clear")
    public String clearCart(HttpSession session) {
        cartService.clear();
        clearDiscount(session);
        return "redirect:/cart";
    }

    // Helper: clear toàn bộ thông tin giảm giá
    private void clearDiscount(HttpSession session) {
        session.removeAttribute("discountCode");
        session.removeAttribute("discountPercent"); // NEW
        session.removeAttribute("discountAmount");
        session.removeAttribute("totalAfterDiscount");
    }
}
