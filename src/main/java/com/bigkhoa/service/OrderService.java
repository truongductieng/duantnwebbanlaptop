package com.bigkhoa.service;

import com.bigkhoa.dto.CheckoutForm;
import com.bigkhoa.dto.RevenueDataDto;
import com.bigkhoa.model.CartItem;
import com.bigkhoa.model.Order;
import com.bigkhoa.model.OrderItem;
import com.bigkhoa.model.OrderStatus;
import com.bigkhoa.model.PaymentMethod;
import com.bigkhoa.model.User;
import com.bigkhoa.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepo;

    /**
     * Tạo đơn hàng mới với customer và status = PENDING.
     */
    public Order createOrder(User customer) {
        Order o = new Order();
        o.setCustomer(customer);
        o.setStatus(OrderStatus.PENDING);
        o.setPaymentMethod(PaymentMethod.COD); // mặc định để tránh null
        o.setTotal(BigDecimal.ZERO);
        return orderRepo.save(o);
    }

    /**
     * Tạo đơn hàng kèm items, thông tin nhận hàng và áp dụng (nếu có) mã giảm giá.
     * GHI NHẬN:
     *   - totalBeforeDiscount = subtotal
     *   - discountCode / discountPercent / discountAmount
     *   - total = subtotal - discountAmount (>= 0)
     *   - paymentMethod (COD/VNPAY/...)
     */
    @Transactional
    public Order createOrder(User customer, List<CartItem> items, CheckoutForm form) {
        Order o = new Order();
        o.setCustomer(customer);
        o.setStatus(OrderStatus.PENDING);

        // Thông tin người nhận
        o.setRecipientName(form.getFullName());
        o.setRecipientEmail(form.getEmail());
        o.setRecipientAddress(form.getAddress());
        o.setRecipientPhone(form.getPhone());

        // === Phương thức thanh toán (mặc định COD nếu form rỗng/không map được) ===
        PaymentMethod pm = PaymentMethod.COD;
        String pmStr = form.getPaymentMethod();
        if (pmStr != null && !pmStr.isBlank()) {
            try {
                pm = PaymentMethod.valueOf(pmStr.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // giữ COD
            }
        }
        o.setPaymentMethod(pm);

        // 1) Tính subtotal (tổng trước giảm)
        BigDecimal subtotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (CartItem ci : items) {
            OrderItem oi = new OrderItem();
            oi.setOrder(o);
            oi.setProduct(ci.getLaptop());
            oi.setQuantity(ci.getQuantity());

            BigDecimal unitPrice = BigDecimal.valueOf(ci.getLaptop().getPrice())
                    .setScale(2, RoundingMode.HALF_UP);
            oi.setUnitPrice(unitPrice);

            subtotal = subtotal.add(unitPrice.multiply(BigDecimal.valueOf(ci.getQuantity())));
            o.getItems().add(oi);
        }
        o.setTotalBeforeDiscount(subtotal);

        // 2) Áp thông tin giảm giá từ form (có thể null)
        Integer percent = form.getDiscountPercent();
        BigDecimal discountAmount = form.getDiscountAmount();
        String code = form.getDiscountCode();

        if (code != null && !code.isBlank()) {
            o.setDiscountCode(code.trim().toUpperCase());
        }

        // Nếu có percent mà amount trống -> tính amount từ percent
        if (discountAmount == null && percent != null && percent > 0) {
            discountAmount = subtotal
                    .multiply(BigDecimal.valueOf(percent))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }

        // Nếu có amount mà percent trống -> suy ra percent để lưu hiển thị
        if ((percent == null || percent <= 0) && discountAmount != null && subtotal.compareTo(BigDecimal.ZERO) > 0) {
            percent = discountAmount
                    .multiply(BigDecimal.valueOf(100))
                    .divide(subtotal, 0, RoundingMode.DOWN)
                    .intValue();
        }

        // Chuẩn hoá default
        if (percent == null) percent = 0;
        if (discountAmount == null) discountAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        // Không cho âm hoặc > subtotal
        if (discountAmount.compareTo(BigDecimal.ZERO) < 0) {
            discountAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (discountAmount.compareTo(subtotal) > 0) {
            discountAmount = subtotal;
        }

        o.setDiscountPercent(percent);
        o.setDiscountAmount(discountAmount);

        // 3) Tính total cuối cùng
        BigDecimal total = subtotal.subtract(discountAmount);
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            total = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        o.setTotal(total);

        return orderRepo.save(o);
    }

    /** Lấy danh sách đơn theo customer. */
    public List<Order> getByCustomer(User user) {
        return orderRepo.findByCustomer(user);
    }

    /** NEW: Lấy danh sách đơn theo customer, kèm join fetch items + product để hiển thị ảnh ở profile. */
    public List<Order> getByCustomerWithItems(User user) {
        return orderRepo.findByCustomerWithItems(user);
    }

    /** Lấy danh sách đơn theo trạng thái. */
    public List<Order> getByStatus(OrderStatus status) {
        return orderRepo.findByStatus(status);
    }

    /** Lấy chi tiết một đơn theo ID (Long). */
    public Order getById(Long orderId) {
        return orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn " + orderId));
    }

    /** Overload cho ID kiểu Integer. */
    public Order getById(Integer id) {
        return getById(id.longValue());
    }

    /** Cập nhật trạng thái đơn. */
    @Transactional
    public Order updateStatus(Long id, OrderStatus newStatus) {
        Order o = getById(id);
        o.setStatus(newStatus);

        // Lưu mốc thời gian giao thành công
        if (newStatus == OrderStatus.DELIVERED) {
            o.setDeliveredAt(LocalDateTime.now());
        }
        return orderRepo.save(o);
    }

    /** Xóa đơn hàng theo ID. */
    @Transactional
    public void deleteOrder(Long id) {
        if (!orderRepo.existsById(id)) {
            throw new IllegalArgumentException("Không tìm thấy đơn để xóa: " + id);
        }
        orderRepo.deleteById(id);
    }

    /** Thống kê tổng doanh thu theo trạng thái. */
    public BigDecimal getTotalRevenue(OrderStatus status) {
        return orderRepo.sumTotalByStatus(status);
    }

    /** Thống kê tổng số lượng sản phẩm đã bán theo trạng thái. */
    public Long getTotalQuantity(OrderStatus status) {
        Long v = orderRepo.sumQuantityByStatus(status);
        return v != null ? v : 0L;
    }

    /** Doanh thu theo ngày. */
    public List<RevenueDataDto> getDailyRevenue(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);
        return orderRepo.revenueDaily(OrderStatus.DELIVERED, start, end).stream()
                .map(row -> new RevenueDataDto(row[0].toString(), (BigDecimal) row[1]))
                .collect(Collectors.toList());
    }

    /** Doanh thu theo tháng. */
    public List<RevenueDataDto> getMonthlyRevenue(LocalDate startMonth, LocalDate endMonth) {
        LocalDateTime start = startMonth.withDayOfMonth(1).atStartOfDay();
        LocalDateTime end   = endMonth.withDayOfMonth(endMonth.lengthOfMonth()).atTime(LocalTime.MAX);

        return orderRepo.revenueMonthly(OrderStatus.DELIVERED, start, end).stream()
                .map(row -> {
                    int y = ((Number) row[0]).intValue();         // year
                    int m = ((Number) row[1]).intValue();         // month
                    BigDecimal sum = (BigDecimal) row[2];         // revenue
                    String ym = y + "-" + String.format("%02d", m);
                    return new RevenueDataDto(ym, sum);
                })
                .collect(Collectors.toList());
    }

    // ================== HỦY ĐƠN HÀNG ==================

    /**
     * Khách tự hủy đơn của chính mình (chỉ khi PENDING hoặc CONFIRMED).
     * @param orderId id đơn
     * @param actor   user đang đăng nhập
     * @param reason  lý do (tùy chọn)
     */
    @Transactional
    public Order cancelOrder(Long orderId, User actor, String reason) {
        Order o = getById(orderId);

        // Chỉ chủ sở hữu mới được hủy
        if (o.getCustomer() == null || actor == null || !o.getCustomer().getId().equals(actor.getId())) {
            throw new AccessDeniedException("Bạn không có quyền hủy đơn này.");
        }

        // Đã hủy rồi -> idempotent
        if (o.getStatus() == OrderStatus.CANCELED) {
            return o;
        }

        // Chỉ cho hủy khi còn trạng thái cho phép
        if (!o.isCancelable()) {
            throw new IllegalStateException("Đơn đã chuyển sang trạng thái không thể hủy.");
        }

        o.setStatus(OrderStatus.CANCELED);
        o.setCanceledAt(LocalDateTime.now());
        o.setCancelReason((reason == null || reason.isBlank()) ? "Khách yêu cầu hủy" : reason.trim());
        o.setCanceledBy("CUSTOMER");

        // Nếu có quản lý kho/thanh toán thì xử lý tại đây:
        // - hoàn kho từ o.getItems()
        // - tạo yêu cầu refund nếu PaymentMethod != COD
        return orderRepo.save(o);
    }

    /**
     * Admin hủy đơn (mặc định cũng chỉ khi PENDING hoặc CONFIRMED để an toàn).
     * Nếu anh muốn admin có thể hủy cả khi SHIPPED, sửa điều kiện theo nhu cầu.
     */
    @Transactional
    public Order cancelOrderByAdmin(Long orderId, String reason) {
        Order o = getById(orderId);

        if (o.getStatus() == OrderStatus.CANCELED) {
            return o;
        }
        if (!o.isCancelable()) {
            throw new IllegalStateException("Đơn đã chuyển sang trạng thái không thể hủy.");
        }

        o.setStatus(OrderStatus.CANCELED);
        o.setCanceledAt(LocalDateTime.now());
        o.setCancelReason((reason == null || reason.isBlank()) ? "Admin hủy đơn" : reason.trim());
        o.setCanceledBy("ADMIN");

        // TODO: Hoàn kho / đánh dấu refund nếu cần
        return orderRepo.save(o);
    }

	public long countByStatus(OrderStatus pending) {
		// TODO Auto-generated method stub
		return 0;
	}
}
