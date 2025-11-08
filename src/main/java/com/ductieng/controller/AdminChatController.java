package com.ductieng.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;

@Controller
@RequestMapping("/admin/chat")
@PreAuthorize("hasRole('ADMIN')")
public class AdminChatController {

    @GetMapping
    public String page(Principal principal, Model model){
        String username = principal != null ? principal.getName() : "ADMIN";
        model.addAttribute("username", username);
        return "admin/chat";
    }
}
