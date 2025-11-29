package com.ductieng.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ductieng.model.Order;
import com.ductieng.model.User;
import com.ductieng.service.OrderService;
import com.ductieng.service.UserService;

@Controller
@RequestMapping("/profile")
public class ProfileController {
    private static final Logger log = LoggerFactory.getLogger(ProfileController.class);

    private final UserService userService;
    private final OrderService orderService;
    private final PasswordEncoder passwordEncoder;
    private final com.ductieng.service.impl.VNPayService vnPayService;

    public ProfileController(UserService userService,
            OrderService orderService,
            PasswordEncoder passwordEncoder,
            com.ductieng.service.impl.VNPayService vnPayService) {
        this.userService = userService;
        this.orderService = orderService;
        this.passwordEncoder = passwordEncoder;
        this.vnPayService = vnPayService;
    }

    // ========================= Helpers =========================
    private boolean isOAuth2(Authentication auth) {
        if (auth == null)
            return false;
        Object p = auth.getPrincipal();
        return (p instanceof OidcUser) || (p instanceof OAuth2User);
    }

    /**
     * Trả về "key" để tìm User trong DB:
     * - Với Google (OIDC/OAuth2): ưu tiên email; nếu không có, fallback
     * auth.getName().
     * - Với local: username.
     */
    private String extractUsernameOrEmail(Authentication auth) {
        if (auth == null)
            return null;
        Object p = auth.getPrincipal();

        if (p instanceof OidcUser oidc) {
            String email = oidc.getEmail();
            if (email != null && !email.isBlank())
                return email;
            return auth.getName(); // fallback
        }
        if (p instanceof OAuth2User ou) {
            Object email = ou.getAttribute("email");
            if (email != null)
                return email.toString();
            return auth.getName(); // fallback (vd: github login)
        }
        if (p instanceof UserDetails ud) {
            return ud.getUsername();
        }
        return auth.getName();
    }

    private User loadUserFromAuth(Authentication authentication) {
        String key = extractUsernameOrEmail(authentication);
        if (key == null)
            return null;

        // Thử tìm theo username trước
        User user = userService.findByUsername(key);
        if (user != null)
            return user;

        // Fallback theo email (cần có hàm trong UserService)
        try {
            User byEmail = userService.findByEmail(key);
            if (byEmail != null)
                return byEmail;
        } catch (Exception ignored) {
        }
        return null;
    }

    // ========================= Endpoints =========================

    @GetMapping
    public String showProfile(Model model, Authentication authentication) {
        User user = loadUserFromAuth(authentication);
        if (user == null) {
            model.addAttribute("error", "Vui lòng đăng nhập để xem hồ sơ.");
            return "redirect:/login";
        }

        try {
            List<Order> orders = orderService.getByCustomerWithItems(user);
            model.addAttribute("orders", orders);
            model.addAttribute("user", user);
            model.addAttribute("oauth2", isOAuth2(authentication));
        } catch (Exception e) {
            log.error("Error loading profile: {}", e.getMessage(), e);
            model.addAttribute("error", "Có lỗi xảy ra khi tải dữ liệu.");
            model.addAttribute("orders", List.of());
            model.addAttribute("user", user);
            model.addAttribute("oauth2", isOAuth2(authentication));
        }

        return "profile";
    }

    @GetMapping("/order/{id}")
    public String viewOrderDetail(@PathVariable("id") Long id,
            Model model,
            Authentication authentication) {
        User user = loadUserFromAuth(authentication);
        if (user == null) {
            model.addAttribute("error", "Tài khoản không hợp lệ.");
            return "redirect:/login";
        }

        Order order = orderService.getById(id);
        if (order == null || !order.getCustomer().getId().equals(user.getId())) {
            model.addAttribute("error", "Không tìm thấy đơn hàng.");
            return "redirect:/profile";
        }

        model.addAttribute("order", order);
        model.addAttribute("items", order.getItems());
        return "order_detail";
    }

    // ==== UPDATE THÔNG TIN CƠ BẢN (username + email + phone) ====
    @PostMapping("/update")
    public String updateProfile(@RequestParam(required = false) String username,
            @RequestParam String email,
            @RequestParam String phone,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {
        User user = loadUserFromAuth(authentication);
        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy tài khoản.");
            return "redirect:/profile";
        }

        boolean oauth2 = isOAuth2(authentication);

        try {
            // Cho phép đổi username nếu KHÔNG phải OAuth2 (Google)
            if (!oauth2 && username != null && !username.isBlank()) {
                user.setUsername(username.trim());
            }
            user.setEmail(email.trim());
            user.setPhone(phone.trim());
            userService.save(user);

            redirectAttributes.addFlashAttribute("message", "Cập nhật thông tin thành công.");
        } catch (Exception e) {
            log.error("Error updating profile: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
        }

        return "redirect:/profile";
    }

    // ==== UPLOAD ẢNH ĐẠI DIỆN ====
    @PostMapping("/avatar")
    public String uploadAvatar(@RequestParam("file") MultipartFile file,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        User user = loadUserFromAuth(authentication);
        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "Tài khoản không hợp lệ.");
            return "redirect:/profile";
        }

        try {
            if (file.isEmpty() || file.getSize() > 2_000_000) {
                redirectAttributes.addFlashAttribute("error", "Ảnh trống hoặc vượt 2MB.");
                return "redirect:/profile";
            }
            String ct = file.getContentType();
            if (ct == null || !(ct.equals("image/png") || ct.equals("image/jpeg"))) {
                redirectAttributes.addFlashAttribute("error", "Chỉ chấp nhận PNG/JPG.");
                return "redirect:/profile";
            }

            String ext = ct.equals("image/png") ? ".png" : ".jpg";
            Path root = Paths.get("src/main/resources/static/uploads/avatars");
            Files.createDirectories(root);

            // Đặt tên theo username
            Path dest = root.resolve(user.getUsername() + ext);
            Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);

            String url = "/uploads/avatars/" + user.getUsername() + ext;
            user.setAvatarUrl(url);
            userService.save(user);

            redirectAttributes.addFlashAttribute("message", "Cập nhật ảnh đại diện thành công.");
        } catch (IOException e) {
            log.error("Error uploading avatar: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Lỗi khi lưu ảnh: " + e.getMessage());
        }
        return "redirect:/profile";
    }

    @GetMapping("/change-password")
    public String showChangePassword(Model model, Authentication authentication) {
        if (isOAuth2(authentication)) {
            model.addAttribute("error", "Tài khoản Google không đổi mật khẩu tại đây.");
        }
        return "change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(@RequestParam String oldPassword,
            @RequestParam String newPassword,
            Model model,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        if (isOAuth2(authentication)) {
            model.addAttribute("error", "Tài khoản Google không hỗ trợ đổi mật khẩu tại đây.");
            return "change-password";
        }

        User user = loadUserFromAuth(authentication);
        if (user == null) {
            model.addAttribute("error", "Không tìm thấy tài khoản.");
            return "change-password";
        }

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            model.addAttribute("error", "Mật khẩu cũ không đúng.");
            return "change-password";
        }

        try {
            // Tránh double-encode: sử dụng service chuyên trách cập nhật mật khẩu
            // vì UserService.save() sẽ tự encode lại password nếu không cẩn thận
            userService.updatePassword(user.getUsername(), newPassword);

            model.addAttribute("message", "Đổi mật khẩu thành công.");
        } catch (Exception e) {
            log.error("Error changing password: {}", e.getMessage(), e);
            model.addAttribute("error", "Có lỗi xảy ra khi đổi mật khẩu.");
        }

        return "change-password";
    }

    // ==== THANH TOÁN LẠI ĐƠN HÀNG (PENDING hoặc CANCELED) ====
    @PostMapping("/order/{id}/retry-payment")
    public String retryPayment(@PathVariable("id") Long orderId,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {
        User user = loadUserFromAuth(authentication);
        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "Tài khoản không hợp lệ.");
            return "redirect:/login";
        }

        try {
            Order order = orderService.getById(orderId);
            log.info("[RetryPayment] User {} attempting to retry payment for order #{}", user.getId(), orderId);

            // Kiểm tra quyền sở hữu đơn hàng
            if (!order.getCustomer().getId().equals(user.getId())) {
                log.warn("[RetryPayment] User {} denied - not owner of order #{}", user.getId(), orderId);
                redirectAttributes.addFlashAttribute("error", "Bạn không có quyền thao tác đơn hàng này.");
                return "redirect:/profile";
            }

            // Chỉ cho phép thanh toán lại với đơn PENDING hoặc CANCELED
            if (order.getStatus() != com.ductieng.model.OrderStatus.PENDING
                    && order.getStatus() != com.ductieng.model.OrderStatus.CANCELED) {
                log.warn("[RetryPayment] Order #{} has invalid status: {}", orderId, order.getStatus());
                redirectAttributes.addFlashAttribute("error",
                        "Chỉ có thể thanh toán lại đơn hàng đang chờ xử lý hoặc đã hủy.");
                return "redirect:/profile/order/" + orderId;
            }

            // Chỉ hỗ trợ thanh toán lại với VNPay
            if (order.getPaymentMethod() != com.ductieng.model.PaymentMethod.VNPAY) {
                log.warn("[RetryPayment] Order #{} has invalid payment method: {}", orderId, order.getPaymentMethod());
                redirectAttributes.addFlashAttribute("error",
                        "Chỉ hỗ trợ thanh toán lại cho đơn hàng VNPay.");
                return "redirect:/profile/order/" + orderId;
            }

            // Tạo link thanh toán VNPay mới
            log.info("[RetryPayment] Creating VNPay payment URL for order #{}, amount: {}", orderId, order.getTotal());
            String paymentUrl = vnPayService.createPayment(
                    order.getTotal(),
                    order.getRecipientName(),
                    order.getId());

            // Cập nhật lại trạng thái về PENDING nếu đang là CANCELED
            if (order.getStatus() == com.ductieng.model.OrderStatus.CANCELED) {
                log.info("[RetryPayment] Updating order #{} from CANCELED to PENDING", orderId);
                orderService.updateStatus(orderId, com.ductieng.model.OrderStatus.PENDING);
            }

            log.info("[RetryPayment] Redirecting to VNPay payment URL for order #{}", orderId);
            return "redirect:" + paymentUrl;

        } catch (Exception e) {
            log.error("[RetryPayment] Error creating payment for order #{}: {}", orderId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error",
                    "Có lỗi xảy ra khi tạo link thanh toán: " + e.getMessage());
            return "redirect:/profile/order/" + orderId;
        }
    }
}
