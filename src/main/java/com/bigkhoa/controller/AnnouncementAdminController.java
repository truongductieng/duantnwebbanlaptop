package com.bigkhoa.controller;

import com.bigkhoa.model.Announcement;
import com.bigkhoa.service.AnnouncementService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.*;

@Controller
@RequestMapping("/admin/ann")
@PreAuthorize("hasRole('ADMIN')")
public class AnnouncementAdminController {

  private final AnnouncementService announcementService;

  public AnnouncementAdminController(AnnouncementService announcementService) {
    this.announcementService = announcementService;
  }

  // DANH SÁCH
  @GetMapping
  public String list(Model model) {
    model.addAttribute("announcements", announcementService.findAll());
    model.addAttribute("active", announcementService.getActive().orElse(null));
    return "admin/ann_list";
  }

  // TẠO MỚI - FORM
  @GetMapping("/new")
  public String createForm(Model model) {
    Announcement a = new Announcement();
    a.setVariant("INFO");
    a.setPosition("TOP");
    a.setEnabled(true);
    model.addAttribute("announcement", a);
    return "admin/ann_form";
  }

  // TẠO MỚI - LƯU
  @PostMapping
  public String create(@ModelAttribute Announcement a,
                       @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                       RedirectAttributes ra) throws IOException {
    handleUpload(a, imageFile);
    announcementService.save(a);
    ra.addFlashAttribute("message", "Đã tạo banner mới");
    return "redirect:/admin/ann";
  }

  // SỬA - FORM
  @GetMapping("/{id}/edit")
  public String editForm(@PathVariable Long id, Model model, RedirectAttributes ra) {
    Announcement a = announcementService.findById(id).orElse(null);
    if (a == null) {
      ra.addFlashAttribute("error", "Không tìm thấy banner #" + id);
      return "redirect:/admin/ann";
    }
    model.addAttribute("announcement", a);
    return "admin/ann_form";
  }

  // SỬA - LƯU
  @PostMapping("/{id}")
  public String update(@PathVariable Long id,
                       @ModelAttribute Announcement a,
                       @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                       RedirectAttributes ra) throws IOException {
    a.setId(id);
    handleUpload(a, imageFile); // nếu có ảnh mới thì cập nhật, không thì giữ nguyên
    announcementService.save(a);
    ra.addFlashAttribute("message", "Đã cập nhật banner #" + id);
    return "redirect:/admin/ann";
  }

  // BẬT/TẮT NHANH (không cần param; tự đảo trạng thái)
  @PostMapping("/{id}/toggle")
  public String toggle(@PathVariable Long id, RedirectAttributes ra) {
    Announcement a = announcementService.findById(id).orElse(null);
    if (a == null) {
      ra.addFlashAttribute("error", "Không tìm thấy banner #" + id);
      return "redirect:/admin/ann";
    }
    boolean newEnabled = !a.isEnabled();
    a.setEnabled(newEnabled);
    announcementService.save(a);
    ra.addFlashAttribute("message", (newEnabled ? "Đã BẬT " : "Đã TẮT ") + "banner #" + id);
    return "redirect:/admin/ann";
  }

  // ❗ CẤM XOÁ KHI ĐANG BẬT
  @PostMapping("/{id}/delete")
  public String delete(@PathVariable Long id, RedirectAttributes ra) {
    Announcement a = announcementService.findById(id).orElse(null);
    if (a == null) {
      ra.addFlashAttribute("error", "Không tìm thấy banner #" + id);
      return "redirect:/admin/ann";
    }
    if (a.isEnabled()) {
      ra.addFlashAttribute("error", "Không thể xóa banner đang BẬT. Vui lòng TẮT trước khi xóa.");
      return "redirect:/admin/ann";
    }
    announcementService.delete(id);
    ra.addFlashAttribute("message", "Đã xóa banner #" + id);
    return "redirect:/admin/ann";
  }

  // UPLOAD ẢNH
  private void handleUpload(Announcement a, MultipartFile imageFile) throws IOException {
    if (imageFile != null && !imageFile.isEmpty()) {
      String uploadsRoot = "uploads/ann";             // thư mục vật lý
      Files.createDirectories(Paths.get(uploadsRoot));
      String original = imageFile.getOriginalFilename() == null ? "image" : imageFile.getOriginalFilename();
      String safeName = System.currentTimeMillis() + "_" + original.replaceAll("\\s+", "_");
      Path target = Paths.get(uploadsRoot).resolve(safeName);
      Files.copy(imageFile.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
      a.setImageUrl("/uploads/ann/" + safeName);     // public URL để view dùng
    }
  }
}
