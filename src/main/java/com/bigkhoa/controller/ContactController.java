package com.bigkhoa.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
// IMPORT ĐÚNG: Model của Spring MVC
import org.springframework.ui.Model;

@Controller
public class ContactController {
  
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
      @RequestParam(required=false) String phone,
      @RequestParam String message,
      Model model) {
    // TODO: lưu DB hoặc gửi email...
    model.addAttribute("success",
        "Cám ơn " + name + ", chúng tôi đã nhận được tin nhắn của bạn!");
    return "contact";
  }
}
