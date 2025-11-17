package com.ductieng.service;

import com.ductieng.model.*;
import com.ductieng.repository.OrderRepository;
import com.ductieng.repository.ReturnRequestRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReturnService {

    @Autowired
    private ReturnRequestRepository returnRequestRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private LaptopService laptopService;

    @Value("${app.return.window-days:14}")
    private int returnWindowDays;

    @Value("${app.upload.return-dir:uploads/returns}")
    private String returnUploadDir;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * DTO cho return item (orderItemId + quantity to return)
     */
    public static class ReturnItemDto {
        public Long orderItemId;
        public int quantity;

        public ReturnItemDto() {
        }

        public ReturnItemDto(Long orderItemId, int quantity) {
            this.orderItemId = orderItemId;
            this.quantity = quantity;
        }
    }

    /**
     * Tạo yêu cầu trả hàng mới
     *
     * @param orderId     ID đơn hàng
     * @param customer    Khách hàng yêu cầu
     * @param reason      Lý do trả hàng
     * @param returnItems Danh sách items muốn trả (JSON: [{orderItemId, quantity}])
     * @param photoFiles  Ảnh chứng minh (có thể null)
     * @return ReturnRequest đã lưu
     */
    @Transactional
    public ReturnRequest createReturnRequest(Long orderId, User customer, String reason,
            List<ReturnItemDto> returnItems, MultipartFile[] photoFiles)
            throws IllegalArgumentException, IOException {

        // 1. Validate order
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Đơn hàng không tồn tại"));

        if (!order.getCustomer().getId().equals(customer.getId())) {
            throw new AccessDeniedException("Bạn không có quyền tạo yêu cầu trả hàng cho đơn này");
        }

        // 2. Kiểm tra trạng thái đơn hàng (chỉ cho phép DELIVERED)
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new IllegalArgumentException("Chỉ có thể trả hàng khi đơn đã giao thành công");
        }

        // 3. Kiểm tra thời hạn trả hàng (deliveredAt + returnWindowDays)
        if (order.getDeliveredAt() == null) {
            throw new IllegalArgumentException("Đơn hàng chưa có thời gian giao hàng");
        }

        LocalDateTime deadline = order.getDeliveredAt().plusDays(returnWindowDays);
        if (LocalDateTime.now().isAfter(deadline)) {
            throw new IllegalArgumentException(
                    String.format("Đã quá thời hạn trả hàng (%d ngày kể từ ngày giao)", returnWindowDays));
        }

        // 4. Kiểm tra xem đơn này đã có yêu cầu trả hàng đang xử lý chưa
        List<ReturnStatus> activeStatuses = Arrays.asList(
                ReturnStatus.REQUESTED, ReturnStatus.APPROVED, ReturnStatus.ITEM_RECEIVED);
        long existingCount = returnRequestRepository.countByOrderIdAndStatusIn(orderId, activeStatuses);
        if (existingCount > 0) {
            throw new IllegalArgumentException("Đơn hàng này đã có yêu cầu trả hàng đang xử lý");
        }

        // 5. Validate returnItems
        if (returnItems == null || returnItems.isEmpty()) {
            throw new IllegalArgumentException("Phải chọn ít nhất một sản phẩm để trả");
        }

        // Tính refundAmount dựa trên returnItems
        BigDecimal refundAmount = calculateRefundAmount(order, returnItems);

        // 6. Tạo ReturnRequest
        ReturnRequest returnRequest = new ReturnRequest();
        returnRequest.setOrder(order);
        returnRequest.setCustomer(customer);
        returnRequest.setReason(reason != null ? reason : "");
        returnRequest.setStatus(ReturnStatus.REQUESTED);
        returnRequest.setRefundAmount(refundAmount);

        // Convert returnItems to JSON
        try {
            String itemsJson = objectMapper.writeValueAsString(returnItems);
            returnRequest.setReturnItemsJson(itemsJson);
        } catch (Exception e) {
            throw new IllegalArgumentException("Lỗi xử lý danh sách sản phẩm trả: " + e.getMessage());
        }

        // 7. Lưu ảnh (nếu có)
        if (photoFiles != null && photoFiles.length > 0) {
            List<String> photoPaths = savePhotos(photoFiles, orderId);
            returnRequest.setPhotos(String.join(",", photoPaths));
        }

        // 8. Lưu vào DB
        return returnRequestRepository.save(returnRequest);
    }

    /**
     * Tính số tiền hoàn trả dựa trên returnItems
     */
    private BigDecimal calculateRefundAmount(Order order, List<ReturnItemDto> returnItems) {
        Map<Long, OrderItem> orderItemMap = order.getItems().stream()
                .collect(Collectors.toMap(OrderItem::getId, item -> item));

        BigDecimal total = BigDecimal.ZERO;

        for (ReturnItemDto dto : returnItems) {
            OrderItem orderItem = orderItemMap.get(dto.orderItemId);
            if (orderItem == null) {
                throw new IllegalArgumentException("Order item không tồn tại: " + dto.orderItemId);
            }
            if (dto.quantity <= 0 || dto.quantity > orderItem.getQuantity()) {
                throw new IllegalArgumentException(
                        "Số lượng trả không hợp lệ cho item " + dto.orderItemId);
            }

            BigDecimal itemRefund = orderItem.getUnitPrice().multiply(BigDecimal.valueOf(dto.quantity));
            total = total.add(itemRefund);
        }

        return total;
    }

    /**
     * Lưu ảnh chứng minh
     */
    private List<String> savePhotos(MultipartFile[] files, Long orderId) throws IOException {
        List<String> paths = new ArrayList<>();

        // Tạo thư mục nếu chưa có
        Path uploadPath = Paths.get(returnUploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        int index = 0;
        for (MultipartFile file : files) {
            if (file.isEmpty())
                continue;

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            String filename = "return_" + orderId + "_" + System.currentTimeMillis() + "_" + index + extension;
            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            paths.add(returnUploadDir + "/" + filename);
            index++;
        }

        return paths;
    }

    /**
     * Lấy tất cả yêu cầu của một khách hàng
     */
    public List<ReturnRequest> getCustomerReturns(User customer) {
        return returnRequestRepository.findByCustomerOrderByCreatedAtDesc(customer);
    }

    /**
     * Lấy một yêu cầu theo ID (kiểm tra quyền)
     */
    public ReturnRequest getReturnRequest(Long id, User customer) {
        ReturnRequest req = returnRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Yêu cầu không tồn tại"));

        if (!req.getCustomer().getId().equals(customer.getId())) {
            throw new AccessDeniedException("Bạn không có quyền xem yêu cầu này");
        }

        return req;
    }

    /**
     * Hủy yêu cầu bởi khách hàng (chỉ khi REQUESTED hoặc APPROVED)
     */
    @Transactional
    public ReturnRequest cancelReturnRequest(Long id, User customer) {
        ReturnRequest req = getReturnRequest(id, customer);

        if (!req.isCancellableByCustomer()) {
            throw new IllegalArgumentException(
                    "Không thể hủy yêu cầu ở trạng thái " + req.getStatus().getLabel());
        }

        req.setStatus(ReturnStatus.CANCELLED);
        return returnRequestRepository.save(req);
    }

    /**
     * [ADMIN] Lấy tất cả yêu cầu
     */
    public List<ReturnRequest> getAllReturns() {
        return returnRequestRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * [ADMIN] Phê duyệt yêu cầu
     */
    @Transactional
    public ReturnRequest approveReturn(Long id, String adminNote) {
        ReturnRequest req = returnRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Yêu cầu không tồn tại"));

        if (req.getStatus() != ReturnStatus.REQUESTED) {
            throw new IllegalArgumentException("Chỉ có thể phê duyệt yêu cầu ở trạng thái 'Đã gửi yêu cầu'");
        }

        req.setStatus(ReturnStatus.APPROVED);
        req.setProcessedAt(LocalDateTime.now());
        if (adminNote != null && !adminNote.isBlank()) {
            req.setAdminNote(adminNote);
        }

        // Chuyển đơn hàng sang trạng thái CANCELED
        Order order = req.getOrder();
        order.setStatus(OrderStatus.CANCELED);
        orderRepository.save(order);

        return returnRequestRepository.save(req);
    }

    /**
     * [ADMIN] Từ chối yêu cầu
     */
    @Transactional
    public ReturnRequest rejectReturn(Long id, String adminNote) {
        ReturnRequest req = returnRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Yêu cầu không tồn tại"));

        if (req.getStatus() != ReturnStatus.REQUESTED) {
            throw new IllegalArgumentException("Chỉ có thể từ chối yêu cầu ở trạng thái 'Đã gửi yêu cầu'");
        }

        req.setStatus(ReturnStatus.REJECTED);
        req.setProcessedAt(LocalDateTime.now());
        req.setAdminNote(adminNote != null ? adminNote : "");

        return returnRequestRepository.save(req);
    }

    /**
     * [ADMIN] Đánh dấu đã nhận hàng trả về
     */
    @Transactional
    public ReturnRequest markAsReceived(Long id) {
        ReturnRequest req = returnRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Yêu cầu không tồn tại"));

        if (req.getStatus() != ReturnStatus.APPROVED) {
            throw new IllegalArgumentException("Chỉ có thể đánh dấu nhận hàng khi yêu cầu đã được phê duyệt");
        }

        req.setStatus(ReturnStatus.ITEM_RECEIVED);
        req.setReceivedAt(LocalDateTime.now());

        // Cập nhật kho (tăng lại stock)
        restoreStock(req);

        return returnRequestRepository.save(req);
    }

    /**
     * [ADMIN] Hoàn tiền
     */
    @Transactional
    public ReturnRequest markAsRefunded(Long id) {
        ReturnRequest req = returnRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Yêu cầu không tồn tại"));

        if (req.getStatus() != ReturnStatus.ITEM_RECEIVED) {
            throw new IllegalArgumentException("Chỉ có thể hoàn tiền khi đã nhận hàng");
        }

        req.setStatus(ReturnStatus.REFUNDED);
        req.setRefundedAt(LocalDateTime.now());

        return returnRequestRepository.save(req);
    }

    /**
     * Cập nhật lại stock khi nhận hàng trả về
     */
    private void restoreStock(ReturnRequest req) {
        try {
            List<ReturnItemDto> returnItems = objectMapper.readValue(
                    req.getReturnItemsJson(), new TypeReference<List<ReturnItemDto>>() {
                    });

            Order order = req.getOrder();
            Map<Long, OrderItem> orderItemMap = order.getItems().stream()
                    .collect(Collectors.toMap(OrderItem::getId, item -> item));

            for (ReturnItemDto dto : returnItems) {
                OrderItem orderItem = orderItemMap.get(dto.orderItemId);
                if (orderItem != null) {
                    Laptop laptop = orderItem.getProduct();
                    int currentQty = laptop.getQuantity() != null ? laptop.getQuantity() : 0;
                    laptop.setQuantity(currentQty + dto.quantity);
                    laptopService.save(laptop);
                }
            }
        } catch (Exception e) {
            // Log error nhưng không throw để không block flow
            System.err.println("Lỗi khi restore stock: " + e.getMessage());
        }
    }

    /**
     * Parse returnItemsJson thành List<ReturnItemDto>
     */
    public List<ReturnItemDto> parseReturnItems(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<ReturnItemDto>>() {
            });
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
