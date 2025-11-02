package com.ductieng.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import com.ductieng.model.*;
import com.ductieng.repository.ReviewRepository;
import com.ductieng.service.OrderService;
import com.ductieng.service.UserService;

import java.util.Map;

@RestController
@RequestMapping("/reviews")
public class OrderReviewController {

    private final OrderService orderService;
    private final UserService userService;
    private final ReviewRepository reviewRepo;

    public OrderReviewController(OrderService orderService, UserService userService, ReviewRepository reviewRepo) {
        this.orderService = orderService;
        this.userService = userService;
        this.reviewRepo = reviewRepo;
    }

    @PostMapping("/orders/{orderId}/laptops/{laptopId}")
    public ResponseEntity<?> create(@AuthenticationPrincipal UserDetails ud,
                                    @PathVariable Long orderId,
                                    @PathVariable Long laptopId,
                                    @RequestBody Map<String, Object> payload){
        User u = userService.findByUsername(ud.getUsername());
        Order order = orderService.getById(orderId);
        if (order == null || !order.getCustomer().getId().equals(u.getId())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Đơn hàng không hợp lệ hoặc không thuộc về bạn."));
        }
        if (order.getStatus() != OrderStatus.DELIVERED) {
            return ResponseEntity.badRequest().body(Map.of("error", "Chỉ đánh giá khi đơn hàng đã giao thành công."));
        }
        boolean exists = order.getItems().stream()
                .anyMatch(i -> i.getProduct() != null && i.getProduct().getId().equals(laptopId));
        if (!exists) {
            return ResponseEntity.badRequest().body(Map.of("error", "Sản phẩm không có trong đơn hàng."));
        }

        int rating = ((Number)payload.getOrDefault("rating", 5)).intValue();
        String comment = (String) payload.getOrDefault("comment", "");

        Review r = new Review();
        r.setUser(u);
        Laptop lp = new Laptop(); lp.setId(laptopId);
        r.setLaptop(lp);
        r.setRating(Math.max(1, Math.min(5, rating)));
        r.setComment(comment);

        // Nếu entity Review của dự án có setOrder(Order) thì liên kết, nếu không có cũng không sao.
        try { r.getClass().getMethod("setOrder", Order.class).invoke(r, order); } catch (Exception ignore) {}

        return ResponseEntity.ok(reviewRepo.save(r));
    }
}
