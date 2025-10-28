package com.bigkhoa.controller;

import com.bigkhoa.model.Order;
import com.bigkhoa.model.OrderStatus;
import com.bigkhoa.model.User;
import com.bigkhoa.repository.OrderRepository;
import com.bigkhoa.service.CartService;
import com.bigkhoa.service.OrderService;
import com.bigkhoa.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/my-orders")
@PreAuthorize("hasAnyRole('USER', 'CUSTOMER', 'ADMIN')")
public class CustomerOrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserService userService;

    @Autowired
    private CartService cartService;

    // NEW: Lưu trực tiếp khi cập nhật shipping
    @Autowired
    private OrderRepository orderRepository;

    // ===== DTO cho form cập nhật địa chỉ giao hàng =====
    public static class UpdateShippingRequest {
        @NotBlank(message = "Tên người nhận không được để trống")
        private String recipientName;

        @NotBlank(message = "Số điện thoại không được để trống")
        @Pattern(regexp = "^(0\\d{9,10}|\\+?\\d{9,12})$", message = "Số điện thoại không hợp lệ")
        private String recipientPhone;

        @NotBlank(message = "Địa chỉ không được để trống")
        private String recipientAddress;

        // getters/setters
        public String getRecipientName() {
            return recipientName;
        }

        public void setRecipientName(String recipientName) {
            this.recipientName = recipientName;
        }

        public String getRecipientPhone() {
            return recipientPhone;
        }

        public void setRecipientPhone(String recipientPhone) {
            this.recipientPhone = recipientPhone;
        }

        public String getRecipientAddress() {
            return recipientAddress;
        }

        public void setRecipientAddress(String recipientAddress) {
            this.recipientAddress = recipientAddress;
        }
    }

    // ===== Danh sách đơn của user =====
    @GetMapping
    public String viewMyOrders(Model model, Principal principal) {
        User u = userService.findByUsername(principal.getName());
        // Nếu muốn hiển thị ảnh sản phẩm trong danh sách, có thể dùng:
        // List<Order> list = orderService.getByCustomerWithItems(u);
        List<Order> list = orderService.getByCustomer(u);
        model.addAttribute("orders", list);
        return "customer/orders";
    }

    // ===== Chi tiết đơn: redirect sang /profile/order/{id} (vì đã xoá view
    // customer/order_detail) =====
    @GetMapping("/{id}")
    public String viewOrderDetail(@PathVariable("id") Integer id,
            Principal principal,
            RedirectAttributes ra) {
        try {
            User u = userService.findByUsername(principal.getName());
            Order order = orderService.getById(id); // overload Integer đã có
            if (order == null || !order.getCustomer().getId().equals(u.getId())) {
                ra.addFlashAttribute("error", "Bạn không có quyền xem đơn #" + id);
                return "redirect:/my-orders";
            }
            return "redirect:/profile/order/" + id;
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Không tìm thấy đơn #" + id);
            return "redirect:/my-orders";
        }
    }

    // ===== Cập nhật địa chỉ giao hàng (chỉ PENDING / CONFIRMED) =====
    @PostMapping("/{id}/shipping")
    public String updateShipping(@PathVariable("id") Integer id,
            @Valid @ModelAttribute("shippingForm") UpdateShippingRequest form,
            BindingResult result,
            Principal principal,
            RedirectAttributes ra) {
        User u = userService.findByUsername(principal.getName());

        // Lấy đơn và kiểm tra quyền sở hữu
        Order order;
        try {
            order = orderService.getById(id);
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Không tìm thấy đơn #" + id);
            return "redirect:/my-orders";
        }
        if (order == null || !order.getCustomer().getId().equals(u.getId())) {
            ra.addFlashAttribute("error", "Bạn không có quyền cập nhật đơn #" + id);
            return "redirect:/my-orders";
        }

        // Chỉ cho phép khi đơn còn PENDING hoặc CONFIRMED
        if (!(order.getStatus() == OrderStatus.PENDING || order.getStatus() == OrderStatus.CONFIRMED)) {
            ra.addFlashAttribute("error", "Đơn hàng không còn được phép chỉnh sửa địa chỉ.");
            return "redirect:/profile/order/" + id;
        }

        // Validate form
        if (result.hasErrors()) {
            ra.addFlashAttribute("org.springframework.validation.BindingResult.shippingForm", result);
            ra.addFlashAttribute("shippingForm", form);
            return "redirect:/profile/order/" + id;
        }

        // Cập nhật và lưu
        order.setRecipientName(form.getRecipientName());
        order.setRecipientPhone(form.getRecipientPhone());
        order.setRecipientAddress(form.getRecipientAddress());
        orderRepository.save(order);

        ra.addFlashAttribute("success", "Cập nhật địa chỉ giao hàng thành công.");
        return "redirect:/profile/order/" + id;
    }

    // ===== Mua lại (chỉ khi đã DELIVERED) =====
    @PostMapping("/{id}/buy-again")
    public String buyAgain(@PathVariable("id") Integer id,
            Authentication auth,
            Principal principal,
            RedirectAttributes ra) {
        User u = userService.findByUsername(principal.getName());
        Order order;
        try {
            order = orderService.getById(id);
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Không tìm thấy đơn #" + id);
            return "redirect:/my-orders";
        }
        if (order == null || !order.getCustomer().getId().equals(u.getId())) {
            ra.addFlashAttribute("error", "Bạn không có quyền thao tác trên đơn #" + id);
            return "redirect:/my-orders";
        }
        if (order.getStatus() != OrderStatus.DELIVERED) {
            ra.addFlashAttribute("error", "Chỉ có thể mua lại khi đơn đã giao thành công.");
            return "redirect:/my-orders";
        }

        order.getItems().forEach(it -> {
            if (it.getProduct() != null) {
                cartService.addToCart(it.getProduct().getId(), it.getQuantity(), auth);
            }
        });

        ra.addFlashAttribute("message", "Đã thêm lại sản phẩm từ đơn #" + id + " vào giỏ hàng.");
        return "redirect:/cart";
    }

    // ===== HỦY ĐƠN (USER) — PENDING/CONFIRMED =====
    @PostMapping("/{id}/cancel")
    public String cancelMyOrder(@PathVariable("id") Integer id,
            @RequestParam(value = "reason", required = false) String reason,
            Principal principal,
            RedirectAttributes ra) {
        User u = userService.findByUsername(principal.getName());
        try {
            orderService.cancelOrder(id.longValue(), u, reason);
            ra.addFlashAttribute("message", "Đã hủy đơn #" + id + " thành công.");
        } catch (AccessDeniedException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        } catch (IllegalStateException | IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Có lỗi khi hủy đơn. Vui lòng thử lại.");
        }
        return "redirect:/my-orders";
    }
}
