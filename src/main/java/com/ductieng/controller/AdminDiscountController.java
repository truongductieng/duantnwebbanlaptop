package com.ductieng.controller;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ductieng.model.Discount;
import com.ductieng.repository.DiscountRepository;
import com.ductieng.repository.OrderRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/discounts")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDiscountController {

    private final DiscountRepository repo;
    private final OrderRepository orderRepo;

    public AdminDiscountController(DiscountRepository repo, OrderRepository orderRepo) {
        this.repo = repo;
        this.orderRepo = orderRepo;
    }

    @GetMapping
    public String list(Model model) {
        List<Discount> list = repo.findAll();

        // Đếm số đơn đã áp mã theo orders.discount_code
        Map<Long, Long> usage = new LinkedHashMap<>();
        for (Discount d : list) {
            String code = d.getCode();
            long used = (code == null || code.isBlank()) ? 0L : orderRepo.countByDiscountCode(code);
            usage.put(d.getId(), used);
        }

        model.addAttribute("list", list);
        model.addAttribute("usage", usage);
        return "admin/discount-list"; // ✅ khớp file discounts-list.html
    }

    @GetMapping("/new")
    public String create(Model model) {
        model.addAttribute("discount", new Discount());
        return "admin/discount-form";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Discount d = repo.findById(id).orElse(null);
        if (d == null) {
            ra.addFlashAttribute("error", "Không tìm thấy mã giảm giá");
            return "redirect:/admin/discounts";
        }
        model.addAttribute("discount", d);
        return "admin/discount-form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("discount") Discount discount,
                       BindingResult result,
                       RedirectAttributes ra,
                       Model model) {
        // Chuẩn hoá CODE để so sánh/đếm đồng nhất
        if (discount.getCode() != null) {
            discount.setCode(discount.getCode().trim().toUpperCase());
        }

        // percent là int -> không check null, chỉ ràng buộc 1..100
        if (discount.getPercent() < 1 || discount.getPercent() > 100) {
            result.rejectValue("percent", "range", "Phần trăm phải từ 1 đến 100");
        }

        if (discount.getStartDate() != null && discount.getEndDate() != null
                && discount.getEndDate().isBefore(discount.getStartDate())) {
            result.rejectValue("endDate", "after", "Ngày kết thúc phải sau hoặc bằng ngày bắt đầu");
        }

        // Check trùng code (ngoại trừ chính nó)
        if (discount.getCode() != null) {
            repo.findByCodeIgnoreCase(discount.getCode()).ifPresent(existing -> {
                if (discount.getId() == null || !existing.getId().equals(discount.getId())) {
                    result.rejectValue("code", "duplicate", "Mã này đã tồn tại");
                }
            });
        }

        if (result.hasErrors()) {
            model.addAttribute("discount", discount);
            return "admin/discount-form";
        }

        repo.save(discount);
        ra.addFlashAttribute("success", "Đã lưu mã giảm giá");
        return "redirect:/admin/discounts";
    }

    // Bật/Tắt nhanh ngay danh sách
    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id, RedirectAttributes ra) {
        Discount d = repo.findById(id).orElse(null);
        if (d == null) {
            ra.addFlashAttribute("error", "Không tìm thấy mã giảm giá");
            return "redirect:/admin/discounts";
        }

        // Field active là boolean -> dùng isActive()
        boolean newActive = !d.isActive();
        d.setActive(newActive);
        repo.saveAndFlush(d); // ✅ flush để đảm bảo cập nhật ngay

        ra.addFlashAttribute("success",
                (newActive ? "Đã BẬT " : "Đã TẮT ") + "mã " + d.getCode());
        return "redirect:/admin/discounts";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        repo.deleteById(id);
        ra.addFlashAttribute("success", "Đã xóa mã giảm giá");
        return "redirect:/admin/discounts";
    }
}
