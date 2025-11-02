package com.bigkhoa.controller;

import com.bigkhoa.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.ui.Model;

@Controller
public class ContactController {

  private static final Logger logger = LoggerFactory.getLogger(ContactController.class);

  private final EmailService emailService;

  @Value("${spring.mail.username}")
  private String adminEmail;

  public ContactController(EmailService emailService) {
    this.emailService = emailService;
  }

  // Hiển thị form
  @GetMapping("/contact")
  public String showContactForm() {
    return "contact";
  }

  // Xử lý submit
  @PostMapping("/contact")
  public String handleContact(
      @RequestParam String name,
      @RequestParam String email,
      @RequestParam(required = false) String phone,
      @RequestParam String message,
      Model model) {

    try {
      // Gửi email thông báo cho admin
      String subject = "[Laptop Shop] Tin nhắn liên hệ từ " + name;
      String content = String.format(
          "Có tin nhắn liên hệ mới từ khách hàng:\n\n" +
              "Họ tên: %s\n" +
              "Email: %s\n" +
              "Số điện thoại: %s\n\n" +
              "Nội dung:\n%s\n\n" +
              "---\n" +
              "Email này được gửi tự động từ form liên hệ trên website.",
          name,
          email,
          (phone != null && !phone.isBlank()) ? phone : "Không cung cấp",
          message);

      emailService.sendSimpleMessage(adminEmail, subject, content);
      logger.info("Đã gửi email liên hệ từ {} ({}) đến admin", name, email);

      // Gửi email xác nhận cho khách hàng
      String customerSubject = "Laptop Shop - Đã nhận được tin nhắn của bạn";
      String customerContent = String.format(
          "Chào %s,\n\n" +
              "Cảm ơn bạn đã liên hệ với Laptop Shop!\n\n" +
              "Chúng tôi đã nhận được tin nhắn của bạn:\n" +
              "\"%s\"\n\n" +
              "Đội ngũ hỗ trợ của chúng tôi sẽ phản hồi trong thời gian sớm nhất.\n\n" +
              "Trân trọng,\n" +
              "Laptop Shop Team",
          name,
          message.length() > 100 ? message.substring(0, 100) + "..." : message);

      emailService.sendSimpleMessage(email, customerSubject, customerContent);
      logger.info("Đã gửi email xác nhận đến khách hàng {}", email);

      model.addAttribute("success",
          "Cảm ơn " + name + "! Chúng tôi đã nhận được tin nhắn và sẽ phản hồi sớm nhất qua email: " + email);

    } catch (MailException | IllegalArgumentException ex) {
      logger.error("Lỗi khi gửi email liên hệ từ {}: {}", email, ex.getMessage(), ex);
      model.addAttribute("error",
          "Đã xảy ra lỗi khi gửi tin nhắn. Vui lòng thử lại sau hoặc liên hệ trực tiếp qua hotline.");
    }

    return "contact";
  }
}
