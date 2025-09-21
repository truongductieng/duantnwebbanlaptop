package com.bigkhoa.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/chat")
@PreAuthorize("hasRole('ADMIN')")
public class AdminChatController {

    @GetMapping
    public String page(){
        return "admin/chat";
    }
}
