package com.ductieng.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Yêu cầu trả hàng
 */
@Entity
@Table(name = "return_requests")
public class ReturnRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Đơn hàng liên quan
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /**
     * Người yêu cầu (chủ đơn hàng)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    /**
     * Trạng thái yêu cầu
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReturnStatus status = ReturnStatus.REQUESTED;

    /**
     * Lý do trả hàng (text tự do)
     */
    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    /**
     * Danh sách item trong đơn bị trả (JSON hoặc relation)
     * Để đơn giản ta lưu JSON: [{"orderItemId":123,"quantity":2}, ...]
     */
    @Column(name = "return_items", columnDefinition = "TEXT")
    private String returnItemsJson;

    /**
     * Danh sách ảnh chứng minh (paths), cách nhau bởi dấu phẩy
     * VD: "uploads/returns/123_1.jpg,uploads/returns/123_2.jpg"
     */
    @Column(name = "photos", columnDefinition = "TEXT")
    private String photos;

    /**
     * Số tiền yêu cầu hoàn trả (tính từ returnItemsJson)
     */
    @Column(name = "refund_amount", precision = 19, scale = 2)
    private BigDecimal refundAmount = BigDecimal.ZERO;

    /**
     * Ghi chú của admin (lý do từ chối / ghi chú nội bộ)
     */
    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;

    /**
     * Thời điểm tạo yêu cầu
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Thời điểm admin xử lý (approve/reject)
     */
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    /**
     * Thời điểm admin đánh dấu "đã nhận hàng"
     */
    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    /**
     * Thời điểm hoàn tiền hoàn tất
     */
    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = ReturnStatus.REQUESTED;
        }
        if (refundAmount == null) {
            refundAmount = BigDecimal.ZERO;
        }
    }

    /**
     * Kiểm tra xem yêu cầu có thể hủy bởi khách không
     * (chỉ khi còn REQUESTED hoặc APPROVED chưa nhận hàng)
     */
    @Transient
    public boolean isCancellableByCustomer() {
        return status == ReturnStatus.REQUESTED || status == ReturnStatus.APPROVED;
    }

    // ========== Getters & Setters ==========

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public User getCustomer() {
        return customer;
    }

    public void setCustomer(User customer) {
        this.customer = customer;
    }

    public ReturnStatus getStatus() {
        return status;
    }

    public void setStatus(ReturnStatus status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getReturnItemsJson() {
        return returnItemsJson;
    }

    public void setReturnItemsJson(String returnItemsJson) {
        this.returnItemsJson = returnItemsJson;
    }

    public String getPhotos() {
        return photos;
    }

    public void setPhotos(String photos) {
        this.photos = photos;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(BigDecimal refundAmount) {
        this.refundAmount = refundAmount;
    }

    public String getAdminNote() {
        return adminNote;
    }

    public void setAdminNote(String adminNote) {
        this.adminNote = adminNote;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }

    public LocalDateTime getRefundedAt() {
        return refundedAt;
    }

    public void setRefundedAt(LocalDateTime refundedAt) {
        this.refundedAt = refundedAt;
    }
}
