package com.bigkhoa.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.bigkhoa.model.Order;
import com.bigkhoa.model.User;
import com.bigkhoa.service.OrderService;
import com.bigkhoa.service.UserService;

@Controller
@RequestMapping("/profile")
public class ProfileController {
    private final UserService userService;
    private final OrderService orderService;
    private final PasswordEncoder passwordEncoder;

    public ProfileController(UserService userService,
                             OrderService orderService,
                             PasswordEncoder passwordEncoder) {
        this.userService     = userService;
        this.orderService    = orderService;
        this.passwordEncoder = passwordEncoder;
    }

    // ========================= Helpers =========================
    private boolean isOAuth2(Authentication auth) {
        if (auth == null) return false;
        Object p = auth.getPrincipal();
        return (p instanceof OidcUser) || (p instanceof OAuth2User);
    }

    /**
     * Trả về "key" để tìm User trong DB:
     * - Với Google (OIDC/OAuth2): ưu tiên email; nếu không có, fallback auth.getName().
     * - Với local: username.
     */
    private String extractUsernameOrEmail(Authentication auth) {
        if (auth == null) return null;
        Object p = auth.getPrincipal();

        if (p instanceof OidcUser oidc) {
            String email = oidc.getEmail();
            if (email != null && !email.isBlank()) return email;
            return auth.getName(); // fallback
        }
        if (p instanceof OAuth2User ou) {
            Object email = ou.getAttribute("email");
            if (email != null) return email.toString();
            return auth.getName(); // fallback (vd: github login)
        }
        if (p instanceof UserDetails ud) {
            return ud.getUsername();
        }
        return auth.getName();
    }

    private User loadUserFromAuth(Authentication authentication) {
        String key = extractUsernameOrEmail(authentication);
        if (key == null) return null;

        // Thử tìm theo username trước
        User user = userService.findByUsername(key);
        if (user != null) return user;

        // Fallback theo email (cần có hàm trong UserService)
        try {
            User byEmail = userService.findByEmail(key);
            if (byEmail != null) return byEmail;
        } catch (Exception ignored) {}
        return null;
    }

    // ========================= Endpoints =========================

    @GetMapping
    public String showProfile(Model model, Authentication authentication) {
        User user = loadUserFromAuth(authentication);
        if (user == null) {
            model.addAttribute("error", "Vui lòng đăng nhập để xem hồ sơ.");
            return "profile";
        }

        List<Order> orders = orderService.getByCustomerWithItems(user);
        model.addAttribute("orders", orders);
        model.addAttribute("user", user);
        model.addAttribute("oauth2", isOAuth2(authentication));
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
                                Model model) {
        User user = loadUserFromAuth(authentication);
        if (user == null) {
            model.addAttribute("error", "Không tìm thấy tài khoản.");
            return "profile";
        }

        boolean oauth2 = isOAuth2(authentication);

        // Cho phép đổi username nếu KHÔNG phải OAuth2 (Google)
        if (!oauth2 && username != null && !username.isBlank()) {
            user.setUsername(username.trim());
        }
        user.setEmail(email.trim());
        user.setPhone(phone.trim());
        userService.save(user);

        model.addAttribute("message", "Cập nhật thông tin thành công.");
        model.addAttribute("user", user);
        model.addAttribute("orders", orderService.getByCustomerWithItems(user));
        model.addAttribute("oauth2", oauth2);
        return "profile";
    }

    // ==== UPLOAD ẢNH ĐẠI DIỆN ====
    @PostMapping("/avatar")
    public String uploadAvatar(@RequestParam("file") MultipartFile file,
                               Authentication authentication,
                               Model model) {
        User user = loadUserFromAuth(authentication);
        if (user == null) {
            model.addAttribute("error", "Tài khoản không hợp lệ.");
            return "redirect:/profile";
        }

        try {
            if (file.isEmpty() || file.getSize() > 2_000_000) {
                model.addAttribute("error", "Ảnh trống hoặc vượt 2MB.");
                return "redirect:/profile";
            }
            String ct = file.getContentType();
            if (ct == null || !(ct.equals("image/png") || ct.equals("image/jpeg"))) {
                model.addAttribute("error", "Chỉ chấp nhận PNG/JPG.");
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

            model.addAttribute("message", "Cập nhật ảnh đại diện thành công.");
        } catch (IOException e) {
            model.addAttribute("error", "Lỗi khi lưu ảnh: " + e.getMessage());
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
                                 Authentication authentication) {
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

        user.setPassword(passwordEncoder.encode(newPassword));
        userService.save(user);

        model.addAttribute("message", "Đổi mật khẩu thành công.");
        return "change-password";
    }
}
