package com.bigkhoa.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
// IMPORT ĐÚNG: Model của Spring MVC
import org.springframework.ui.Model;

import com.bigkhoa.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.regex.Pattern;

@Controller
public class ContactController {

  private final EmailService emailService;
  private static final Logger log = LoggerFactory.getLogger(ContactController.class);
  private static final int MAX_NAME_LEN = 100;
  private static final int MAX_MESSAGE_LEN = 2000;
  private static final int MAX_PHONE_LEN = 20;
  private static final Pattern SIMPLE_EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

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

    // Basic sanitization & validation
    name = (name == null) ? "" : name.trim();
    email = (email == null) ? "" : email.trim();
    phone = (phone == null) ? "" : phone.trim();
    message = (message == null) ? "" : message.trim();

    // Reject header injection attempts
    if (containsNewline(name) || containsNewline(email) || containsNewline(phone)) {
      model.addAttribute("error", "Dữ liệu gửi lên không hợp lệ.");
      return "contact";
    }

    if (name.isEmpty() || name.length() > MAX_NAME_LEN) {
      model.addAttribute("error", "Tên không hợp lệ (1 - " + MAX_NAME_LEN + " ký tự).");
      return "contact";
    }

    if (!isValidEmail(email)) {
      model.addAttribute("error", "Email không hợp lệ.");
      return "contact";
    }

    if (!phone.isEmpty()) {
      String digits = phone.replaceAll("[^0-9+\\-() ]", "");
      if (digits.length() > MAX_PHONE_LEN) {
        model.addAttribute("error", "Số điện thoại quá dài.");
        return "contact";
      }
      // normalize phone to allowed chars
      phone = digits;
    }

    if (message.isEmpty() || message.length() > MAX_MESSAGE_LEN) {
      model.addAttribute("error", "Nội dung tin nhắn không hợp lệ (1 - " + MAX_MESSAGE_LEN + " ký tự).");
      return "contact";
    }

    // Truncate message to max allowed to avoid huge payloads in logs or emails
    if (message.length() > MAX_MESSAGE_LEN) {
      message = message.substring(0, MAX_MESSAGE_LEN);
    }

    try {
      String subject = "[Laptop Shop] Tin nhắn liên hệ từ " + name;
      String content = String.format("""
          Bạn có tin nhắn liên hệ mới từ:

          Họ tên: %s
          Email: %s
          Điện thoại: %s

          Nội dung tin nhắn:
          %s
          """, name, email, phone != null && !phone.isEmpty() ? phone : "Không cung cấp", message);

      // Log only metadata; avoid logging message body or other PII
      log.info("[Contact] Gửi email liên hệ tới {} (from={} phoneLen={})", "ductieng4231@gmail.com", maskEmail(email),
          phone == null ? 0 : phone.length());
      emailService.sendSimpleMessage("ductieng4231@gmail.com", subject, content);

      model.addAttribute("success",
          "Cám ơn " + name + ", chúng tôi đã nhận được tin nhắn của bạn và sẽ phản hồi sớm nhất!");

    } catch (Exception e) {
      // Log full stacktrace so we can diagnose the mail failure
      log.error("[Contact] Lỗi khi gửi email liên hệ: {}", e.getMessage(), e);
      model.addAttribute("error",
          "Xin lỗi, đã có lỗi xảy ra khi gửi tin nhắn. Vui lòng thử lại sau.");
    }

    return "contact";
  }

  // Helper: prevent header injection
  private boolean containsNewline(String s) {
    return s != null && (s.contains("\n") || s.contains("\r"));
  }

  private boolean isValidEmail(String email) {
    if (email == null || email.isBlank())
      return false;
    if (email.length() > 254)
      return false;
    if (containsNewline(email))
      return false;
    return SIMPLE_EMAIL.matcher(email).matches();
  }

  private String maskEmail(String email) {
    if (email == null)
      return null;
    int at = email.indexOf('@');
    if (at <= 1)
      return "***";
    return email.substring(0, 1) + "***" + email.substring(at - 1);
  }
}
