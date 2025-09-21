package com.bigkhoa.controller;

import com.bigkhoa.dto.CheckoutForm;
import com.bigkhoa.model.CartItem;
import com.bigkhoa.model.Order;
import com.bigkhoa.model.OrderStatus;
import com.bigkhoa.model.User;
import com.bigkhoa.service.CartService;
import com.bigkhoa.service.OrderService;
import com.bigkhoa.service.UserService;
import com.bigkhoa.service.impl.GmailService;
import com.bigkhoa.service.impl.VNPayService;

import jakarta.servlet.http.HttpSession;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CheckoutController {

    private static final Logger log = LoggerFactory.getLogger(CheckoutController.class);

    @Autowired private CartService cartService;
    @Autowired private OrderService orderService;
    @Autowired private UserService userService;
    @Autowired private VNPayService vnpService;
    @Autowired private GmailService gmailService;

    // ===== Helpers cho OAuth2/UserDetails =====
    private String extractUsernameOrEmail(Authentication auth) {
        if (auth == null) return null;
        Object p = auth.getPrincipal();
        if (p instanceof OidcUser oidc) {
            if (oidc.getEmail() != null && !oidc.getEmail().isBlank()) return oidc.getEmail();
            return auth.getName();
        }
        if (p instanceof OAuth2User ou) {
            Object email = ou.getAttribute("email");
            return email != null ? email.toString() : auth.getName();
        }
        if (p instanceof UserDetails ud) {
            return ud.getUsername();
        }
        return auth.getName();
    }

    /** Tải User từ DB theo username trước, nếu không có thì thử theo email. */
    private User loadUserFromAuth(Authentication authentication) {
        String key = extractUsernameOrEmail(authentication);
        if (key == null) return null;

        User u = userService.findByUsername(key);
        if (u != null) return u;

        try {
            return userService.findByEmail(key);
        } catch (Exception ignored) {
            return null;
        }
    }

    @GetMapping("/checkout")
    public String showCheckoutForm(Model model, HttpSession session) {
        // 1) Lấy giỏ & tổng gốc
        List<CartItem> items = cartService.getItems();
        model.addAttribute("cartItems", items);

        BigDecimal totalPrice = items.stream()
            .map(i -> BigDecimal.valueOf(i.getLaptop().getPrice())
                                 .multiply(BigDecimal.valueOf(i.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);
        model.addAttribute("totalPrice", totalPrice);

        // 2) Lấy mã/percent từ session
        String discountCode = (String) session.getAttribute("discountCode");
        Integer discountPercent = null;
        Object p = session.getAttribute("discountPercent");
        if (p instanceof Integer) discountPercent = (Integer) p;

        // 3) Tính amount
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (discountPercent != null && discountPercent > 0) {
            discountAmount = totalPrice
                .multiply(BigDecimal.valueOf(discountPercent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            Object dAmt = session.getAttribute("discountAmount");
            if (dAmt instanceof Long) {
                discountAmount = BigDecimal.valueOf((Long) dAmt).setScale(2, RoundingMode.HALF_UP);
            }
        }
        if (discountAmount.compareTo(BigDecimal.ZERO) < 0) discountAmount = BigDecimal.ZERO;
        if (discountAmount.compareTo(totalPrice) > 0) discountAmount = totalPrice;

        BigDecimal totalAfterDiscount = totalPrice.subtract(discountAmount).setScale(2, RoundingMode.HALF_UP);

        // 4) Pre-populate form
        CheckoutForm form = new CheckoutForm();
        form.setDiscountCode(discountCode);
        form.setDiscountPercent(discountPercent != null ? discountPercent : 0);
        form.setDiscountAmount(discountAmount);
        form.setTotalAfterDiscount(totalAfterDiscount);

        // 5) Model cho view
        model.addAttribute("discountCode", discountCode);
        model.addAttribute("discountAmount", discountAmount);
        model.addAttribute("totalAfterDiscount", totalAfterDiscount);
        model.addAttribute("checkoutForm", form);

        return "checkout";
    }

    @PostMapping("/checkout")
    public String processCheckout(
            @ModelAttribute("checkoutForm") CheckoutForm form,
            Authentication authentication,           // <— dùng Authentication thay vì @AuthenticationPrincipal UserDetails
            HttpSession session,
            RedirectAttributes ra
    ) throws UnsupportedEncodingException {

        // 1) Lấy user an toàn cho cả local & Google
        User u = loadUserFromAuth(authentication);
        if (u == null) {
            ra.addFlashAttribute("message", "Vui lòng đăng nhập lại trước khi thanh toán.");
            return "redirect:/login";
        }

        // 2) Bảo vệ: lấy lại discount từ session (không tin hidden)
        String discountCode = (String) session.getAttribute("discountCode");
        Integer discountPercent = null;
        Object p = session.getAttribute("discountPercent");
        if (p instanceof Integer) discountPercent = (Integer) p;

        // Tính lại amount theo percent dựa trên giỏ hiện tại
        List<CartItem> items = cartService.getItems();
        BigDecimal totalPrice = items.stream()
            .map(i -> BigDecimal.valueOf(i.getLaptop().getPrice())
                                 .multiply(BigDecimal.valueOf(i.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);

        BigDecimal discountAmount = BigDecimal.ZERO;
        if (discountPercent != null && discountPercent > 0) {
            discountAmount = totalPrice
                    .multiply(BigDecimal.valueOf(discountPercent))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }

        BigDecimal totalAfterDiscount = totalPrice.subtract(discountAmount);
        if (totalAfterDiscount.compareTo(BigDecimal.ZERO) < 0) {
            totalAfterDiscount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        // Gán lại vào form để OrderService ghi nhận đầy đủ
        form.setDiscountCode(discountCode);
        form.setDiscountPercent(discountPercent != null ? discountPercent : 0);
        form.setDiscountAmount(discountAmount);
        form.setTotalAfterDiscount(totalAfterDiscount);

        // 3) Tạo order
        Order order = orderService.createOrder(u, items, form);

        // 3.5) Gửi mail sau khi tạo đơn
        log.info("[Checkout] Send initial confirmation email for order #{}", order.getId());
        gmailService.sendOrderConfirmationEmail(order);

        // 4) Thanh toán
        if ("VNPAY".equalsIgnoreCase(form.getPaymentMethod())) {
            String payUrl = vnpService.createPayment(order.getTotal(), form.getFullName(), order.getId());
            log.info("[Checkout] Created order #{} (VNPAY) -> redirect to VNPay", order.getId());

            // clear cart + session giảm giá
            cartService.clear();
            session.removeAttribute("discountCode");
            session.removeAttribute("discountPercent");
            session.removeAttribute("discountAmount");
            session.removeAttribute("totalAfterDiscount");
            return "redirect:" + payUrl;
        } else {
            // COD
            ra.addFlashAttribute("message", "Đặt hàng thành công!");
            cartService.clear();
            session.removeAttribute("discountCode");
            session.removeAttribute("discountPercent");
            session.removeAttribute("discountAmount");
            session.removeAttribute("totalAfterDiscount");
            return "redirect:/confirmation/" + order.getId();
        }
    }

    @GetMapping("/payment")
    public String handleVNPayReturn(
            @RequestParam Map<String, String> params,
            RedirectAttributes redirectAttributes
    ) {
        String responseCode = params.get("vnp_ResponseCode");
        Long orderId = Long.valueOf(params.get("vnp_TxnRef"));

        if ("00".equals(responseCode)) {
            orderService.updateStatus(orderId, OrderStatus.CONFIRMED);
            log.info("[VNPay] Thanh toán thành công cho đơn #{} -> gửi mail xác nhận (CONFIRMED)", orderId);

            gmailService.sendOrderConfirmationEmail(orderService.getById(orderId));

            redirectAttributes.addFlashAttribute(
                "message", "Thanh toán VNPay thành công cho đơn #" + orderId);
        } else {
            orderService.updateStatus(orderId, OrderStatus.PENDING);
            redirectAttributes.addFlashAttribute(
                "message", "Thanh toán thất bại. Vui lòng thử lại đơn #" + orderId);
        }
        return "redirect:/confirmation/" + orderId;
    }

    @GetMapping("/confirmation/{orderId}")
    public String showConfirmation(@PathVariable Long orderId, Model model) {
        model.addAttribute("order", orderService.getById(orderId));
        return "confirmation";
    }
}
