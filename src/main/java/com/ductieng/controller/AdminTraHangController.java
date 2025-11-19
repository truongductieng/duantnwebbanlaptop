package com.ductieng.controller;

import com.ductieng.model.ReturnRequest;
import com.ductieng.model.ReturnStatus;
import com.ductieng.service.ReturnService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller quản lý trả hàng (Admin)
 */
@Controller
@RequestMapping("/admin/returns")
@PreAuthorize("hasRole('ADMIN')")
public class AdminTraHangController {

    @Autowired
    private ReturnService returnService;

    /**
     * Trang danh sách yêu cầu trả hàng
     * GET /admin/returns
     */
    @GetMapping
    public String listReturns(
            @RequestParam(value = "status", required = false) String statusFilter,
            Model model) {

        List<ReturnRequest> returns;

        if (statusFilter != null && !statusFilter.isEmpty() && !statusFilter.equals("ALL")) {
            try {
                ReturnStatus status = ReturnStatus.valueOf(statusFilter);
                returns = returnService.getAllReturns().stream()
                        .filter(r -> r.getStatus() == status)
                        .toList();
            } catch (IllegalArgumentException e) {
                returns = returnService.getAllReturns();
            }
        } else {
            returns = returnService.getAllReturns();
        }

        model.addAttribute("returns", returns);
        model.addAttribute("statusFilter", statusFilter != null ? statusFilter : "ALL");
        model.addAttribute("allStatuses", ReturnStatus.values());

        return "admin/returns";
    }

    /**
     * Trang chi tiết yêu cầu trả hàng
     * GET /admin/returns/{id}
     */
    @GetMapping("/{id}")
    public String viewReturnDetail(@PathVariable Long id, Model model, RedirectAttributes ra) {
        try {
            ReturnRequest returnRequest = returnService.getAllReturns().stream()
                    .filter(r -> r.getId().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Yêu cầu không tồn tại"));

            model.addAttribute("returnRequest", returnRequest);
            model.addAttribute("returnItems", returnService.parseReturnItems(returnRequest.getReturnItemsJson()));

            return "admin/return-detail";

        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/returns";
        }
    }

    /**
     * API: Phê duyệt yêu cầu
     * POST /admin/returns/{id}/approve
     */
    @PostMapping("/{id}/approve")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> approveReturn(
            @PathVariable Long id,
            @RequestParam(value = "adminNote", required = false) String adminNote) {

        try {
            ReturnRequest updated = returnService.approveReturn(id, adminNote);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Yêu cầu đã được phê duyệt");
            response.put("status", updated.getStatus().name());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * API: Từ chối yêu cầu
     * POST /admin/returns/{id}/reject
     */
    @PostMapping("/{id}/reject")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> rejectReturn(
            @PathVariable Long id,
            @RequestParam(value = "adminNote", required = false) String adminNote) {

        try {
            if (adminNote == null || adminNote.isBlank()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Vui lòng nhập lý do từ chối");
                return ResponseEntity.badRequest().body(error);
            }

            ReturnRequest updated = returnService.rejectReturn(id, adminNote);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Yêu cầu đã bị từ chối");
            response.put("status", updated.getStatus().name());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * API: Đánh dấu đã nhận hàng
     * POST /admin/returns/{id}/received
     */
    @PostMapping("/{id}/received")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> markAsReceived(@PathVariable Long id) {
        try {
            ReturnRequest updated = returnService.markAsReceived(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Đã đánh dấu nhận hàng và cập nhật kho");
            response.put("status", updated.getStatus().name());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * API: Hoàn tiền
     * POST /admin/returns/{id}/refund
     */
    @PostMapping("/{id}/refund")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> markAsRefunded(@PathVariable Long id) {
        try {
            ReturnRequest updated = returnService.markAsRefunded(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Đã hoàn tiền thành công");
            response.put("status", updated.getStatus().name());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
