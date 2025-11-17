package com.ductieng.controller;

import com.ductieng.model.ReturnRequest;
import com.ductieng.model.User;
import com.ductieng.service.ReturnService;
import com.ductieng.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API cho chức năng trả hàng (khách hàng)
 */
@RestController
@RequestMapping("/api/returns")
public class ReturnRequestController {

    @Autowired
    private ReturnService returnService;

    @Autowired
    private UserService userService;

    /**
     * Tạo yêu cầu trả hàng mới
     *
     * POST /api/returns/request
     * Form data: orderId, reason, returnItems (JSON string), photos[] (files)
     */
    @PostMapping("/request")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> createReturnRequest(
            @RequestParam("orderId") Long orderId,
            @RequestParam(value = "reason", required = false) String reason,
            @RequestParam("returnItems") String returnItemsJson,
            @RequestParam(value = "photos", required = false) MultipartFile[] photos,
            Principal principal) {

        try {
            User customer = userService.findByUsername(principal.getName());
            if (customer == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Người dùng không tồn tại");
            }

            // Parse returnItems JSON
            List<ReturnService.ReturnItemDto> returnItems = returnService.parseReturnItems(returnItemsJson);

            ReturnRequest returnRequest = returnService.createReturnRequest(
                    orderId, customer, reason, returnItems, photos);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Yêu cầu trả hàng đã được gửi thành công");
            response.put("returnRequestId", returnRequest.getId());
            response.put("status", returnRequest.getStatus().name());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Có lỗi xảy ra: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Lấy danh sách yêu cầu trả hàng của khách hàng
     *
     * GET /api/returns/my
     */
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<ReturnRequest>> getMyReturns(Principal principal) {
        User customer = userService.findByUsername(principal.getName());
        if (customer == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        List<ReturnRequest> returns = returnService.getCustomerReturns(customer);
        return ResponseEntity.ok(returns);
    }

    /**
     * Lấy chi tiết một yêu cầu trả hàng
     *
     * GET /api/returns/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ReturnRequest> getReturnRequest(@PathVariable Long id, Principal principal) {
        try {
            User customer = userService.findByUsername(principal.getName());
            if (customer == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
            }

            ReturnRequest returnRequest = returnService.getReturnRequest(id, customer);
            return ResponseEntity.ok(returnRequest);

        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        }
    }

    /**
     * Hủy yêu cầu trả hàng
     *
     * POST /api/returns/{id}/cancel
     */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> cancelReturn(@PathVariable Long id, Principal principal) {
        try {
            User customer = userService.findByUsername(principal.getName());
            if (customer == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
            }

            ReturnRequest returnRequest = returnService.cancelReturnRequest(id, customer);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Yêu cầu đã được hủy");
            response.put("status", returnRequest.getStatus().name());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);

        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        }
    }
}
