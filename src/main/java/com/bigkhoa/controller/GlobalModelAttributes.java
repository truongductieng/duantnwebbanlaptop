package com.bigkhoa.controller;

import com.bigkhoa.service.CartService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.ui.Model;

@ControllerAdvice
public class GlobalModelAttributes {

    private final CartService cartService;

    public GlobalModelAttributes(CartService cartService) {
        this.cartService = cartService;
    }

    @ModelAttribute
    public void addGlobalAttributes(Model m) {
        m.addAttribute("cartItemCount", cartService.getItemCount());
    }
}
