package com.ductieng.controller;

import com.ductieng.model.Brand;
import com.ductieng.service.BrandService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/admin/brands")
public class BrandController {

    private final BrandService brandService;

    public BrandController(BrandService brandService) {
        this.brandService = brandService;
    }

    /**
     * Hiển thị danh sách brands
     */
    @GetMapping
    public String list(Model model) {
        model.addAttribute("brands", brandService.getAllBrands());
        return "admin/brand-list";
    }

    /**
     * Hiển thị form thêm brand mới
     */
    @GetMapping("/new")
    public String showNewForm(Model model) {
        model.addAttribute("brand", new Brand());
        model.addAttribute("isEdit", false);
        return "admin/brand-form";
    }

    /**
     * Hiển thị form sửa brand
     */
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes ra) {
        return brandService.findById(id)
                .map(brand -> {
                    model.addAttribute("brand", brand);
                    model.addAttribute("isEdit", true);
                    return "admin/brand-form";
                })
                .orElseGet(() -> {
                    ra.addFlashAttribute("error", "Không tìm thấy brand với ID: " + id);
                    return "redirect:/admin/brands";
                });
    }

    /**
     * Xử lý tạo mới hoặc cập nhật brand
     */
    @PostMapping("/save")
    public String save(@ModelAttribute Brand brand,
            BindingResult result,
            RedirectAttributes ra) {

        // Validate tên brand
        if (brand.getName() == null || brand.getName().trim().isEmpty()) {
            ra.addFlashAttribute("error", "Tên brand không được để trống");
            return "redirect:/admin/brands/new";
        }

        // Kiểm tra trùng tên
        if (brand.getId() == null) {
            // Thêm mới
            if (brandService.existsByName(brand.getName())) {
                ra.addFlashAttribute("error", "Brand '" + brand.getName() + "' đã tồn tại");
                return "redirect:/admin/brands/new";
            }
        } else {
            // Cập nhật
            if (brandService.existsByNameAndNotId(brand.getName(), brand.getId())) {
                ra.addFlashAttribute("error", "Brand '" + brand.getName() + "' đã tồn tại");
                return "redirect:/admin/brands/" + brand.getId() + "/edit";
            }
        }

        try {
            brandService.save(brand);
            ra.addFlashAttribute("success",
                    brand.getId() == null ? "Thêm brand thành công" : "Cập nhật brand thành công");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi khi lưu brand: " + e.getMessage());
        }

        return "redirect:/admin/brands";
    }

    /**
     * Xóa brand
     */
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            if (brandService.findById(id).isPresent()) {
                brandService.deleteById(id);
                ra.addFlashAttribute("success", "Xóa brand thành công");
            } else {
                ra.addFlashAttribute("error", "Không tìm thấy brand với ID: " + id);
            }
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi khi xóa brand: " + e.getMessage());
        }
        return "redirect:/admin/brands";
    }
}
