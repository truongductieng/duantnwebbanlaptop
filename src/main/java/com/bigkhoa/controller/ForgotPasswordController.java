package com.bigkhoa.controller;

import com.bigkhoa.model.PasswordResetToken;
import com.bigkhoa.model.User;
import com.bigkhoa.repository.PasswordResetTokenRepository;
import com.bigkhoa.service.EmailService;
import com.bigkhoa.service.UserService;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class ForgotPasswordController {
    private static final Logger logger = LoggerFactory.getLogger(ForgotPasswordController.class);

    private final UserService userService;
    private final PasswordResetTokenRepository tokenRepo;
    private final EmailService emailService;

    public ForgotPasswordController(UserService userService,
            PasswordResetTokenRepository tokenRepo,
            EmailService emailService) {
        this.userService = userService;
        this.tokenRepo = tokenRepo;
        this.emailService = emailService;
    }

    @GetMapping("/forgot-password")
    public String showForgotForm() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgot(@RequestParam("identifier") String identifier,
            Model model, HttpServletRequest request) {
        String input = identifier == null ? "" : identifier.trim();

        // Cho phép nhập email hoặc username
        User user = input.contains("@")
                ? userService.findByEmail(input)
                : userService.findByUsername(input);

        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            logger.warn("Không tìm thấy tài khoản với identifier: {}", input);
            model.addAttribute("error", "Không tìm thấy tài khoản hoặc tài khoản chưa có email.");
            return "forgot-password";
        }

        logger.info("Tìm thấy user: {} với email: {}", user.getUsername(), user.getEmail());

        // Xoá mọi token cũ của user
        tokenRepo.deleteByUserId(user.getId());

        // Tạo token mới
        String token = UUID.randomUUID().toString();
        PasswordResetToken pr = new PasswordResetToken();
        pr.setToken(token);
        pr.setUser(user);
        pr.setExpiryDate(LocalDateTime.now().plusHours(1));
        tokenRepo.save(pr);

        // Tạo link reset tuyệt đối theo host hiện tại
        String resetLink = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/reset-password")
                .queryParam("token", token)
                .toUriString();

        logger.info("Link reset password: {}", resetLink);

        try {
            emailService.sendSimpleMessage(
                    user.getEmail(),
                    "Đặt lại mật khẩu - Laptop Shop",
                    "Chào " + (user.getFullName() != null ? user.getFullName() : user.getUsername()) + ",\n\n"
                            + "Bạn đã yêu cầu đặt lại mật khẩu.\n"
                            + "Nhấp vào liên kết sau để đặt lại mật khẩu (hết hạn sau 1 giờ):\n"
                            + resetLink + "\n\n"
                            + "Nếu bạn không yêu cầu, vui lòng bỏ qua email này.");
            logger.info("Email đã được gửi thành công đến: {}", mask(user.getEmail()));
            model.addAttribute("message", "Đã gửi email hướng dẫn đến " + mask(user.getEmail()));
        } catch (MailException | IllegalArgumentException ex) {
            logger.error("Lỗi khi gửi email: {}", ex.getMessage(), ex);
            model.addAttribute("error", "Không gửi được email: " + ex.getMessage() + ". Vui lòng thử lại sau.");
        }
        return "forgot-password";
    }

    @GetMapping("/reset-password")
    public String showResetForm(@RequestParam String token, Model model) {
        Optional<PasswordResetToken> optionalPr = tokenRepo.findByToken(token);
        if (optionalPr.isEmpty() ||
                optionalPr.get().getExpiryDate().isBefore(LocalDateTime.now())) {
            model.addAttribute("error", "Liên kết không hợp lệ hoặc đã hết hạn.");
            return "forgot-password";
        }
        model.addAttribute("token", token);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String processReset(@RequestParam String token,
            @RequestParam String password,
            Model model) {
        Optional<PasswordResetToken> optionalPr = tokenRepo.findByToken(token);
        if (optionalPr.isEmpty() ||
                optionalPr.get().getExpiryDate().isBefore(LocalDateTime.now())) {
            model.addAttribute("error", "Liên kết không hợp lệ hoặc đã hết hạn.");
            return "forgot-password";
        }

        User user = optionalPr.get().getUser();
        // updatePassword phải encode BCrypt bên trong
        userService.updatePassword(user.getUsername(), password);
        tokenRepo.delete(optionalPr.get());

        model.addAttribute("message", "Đổi mật khẩu thành công. Vui lòng đăng nhập lại.");
        return "login";
    }

    // Ẩn bớt email trong thông báo
    private static String mask(String email) {
        int at = email.indexOf('@');
        if (at <= 1)
            return "***" + email.substring(at);
        return email.charAt(0) + "***" + email.substring(at - 1);
    }
}
