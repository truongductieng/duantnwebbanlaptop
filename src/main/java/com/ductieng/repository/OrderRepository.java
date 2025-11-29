package com.ductieng.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.ductieng.dto.TopProductDto;
import com.ductieng.model.Order;
import com.ductieng.model.OrderStatus;
import com.ductieng.model.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

  List<Order> findByCustomer(User customer);

  List<Order> findByStatus(OrderStatus status);

  // Tìm đơn hàng theo customer và status
  List<Order> findByCustomerAndStatus(User customer, OrderStatus status);

  // Xóa tất cả đơn hàng của user
  @Modifying
  @Transactional
  @Query("DELETE FROM Order o WHERE o.customer.id = :userId")
  void deleteByCustomerId(@Param("userId") Long userId);

  // Tổng doanh thu của các đơn theo 1 trạng thái
  @Query("SELECT COALESCE(SUM(o.total), 0) FROM Order o WHERE o.status = :status")
  BigDecimal sumTotalByStatus(@Param("status") OrderStatus status);

  // Tổng số lượng sản phẩm đã bán theo 1 trạng thái
  @Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM Order o JOIN o.items oi WHERE o.status = :status")
  Long sumQuantityByStatus(@Param("status") OrderStatus status);

  // Doanh thu theo ngày trong khoảng (1 trạng thái)
  @Query("""
      SELECT function('date', o.createdAt) as day,
             COALESCE(SUM(o.total),0)
      FROM Order o
      WHERE o.status = :status
        AND o.createdAt BETWEEN :start AND :end
      GROUP BY function('date', o.createdAt)
      ORDER BY function('date', o.createdAt)
      """)
  List<Object[]> revenueDaily(
      @Param("status") OrderStatus status,
      @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end);

  // Doanh thu theo THÁNG (1 trạng thái)
  @Query("""
      SELECT function('year', o.createdAt)  as y,
             function('month', o.createdAt) as m,
             COALESCE(SUM(o.total),0)
      FROM Order o
      WHERE o.status = :status
        AND o.createdAt BETWEEN :start AND :end
      GROUP BY function('year', o.createdAt), function('month', o.createdAt)
      ORDER BY function('year', o.createdAt), function('month', o.createdAt)
      """)
  List<Object[]> revenueMonthly(
      @Param("status") OrderStatus status,
      @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end);

  // Lấy đơn theo user kèm items & product
  @Query("""
        select distinct o from Order o
        left join fetch o.items i
        left join fetch i.product p
        where o.customer = :user
        order by o.createdAt desc
      """)
  List<Order> findByCustomerWithItems(@Param("user") User user);

  // Nạp 1 đơn theo id kèm items + product + customer
  @Query("""
        select distinct o from Order o
        left join fetch o.customer c
        left join fetch o.items i
        left join fetch i.product p
        where o.id = :id
      """)
  Optional<Order> findByIdWithItems(@Param("id") Long id);

  // Đếm số đơn áp 1 mã giảm giá
  long countByDiscountCode(String discountCode);

  // ==================== DASHBOARD (nhiều trạng thái) ====================

  // Tổng doanh thu trong khoảng theo nhiều trạng thái
  @Query("""
      select coalesce(sum(o.total), 0)
      from Order o
      where o.createdAt >= :start and o.createdAt < :end
        and o.status in :statuses
      """)
  BigDecimal sumTotalBetween(@Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end,
      @Param("statuses") List<OrderStatus> statuses);

  // Doanh thu theo NGÀY trong khoảng theo nhiều trạng thái
  @Query("""
      select function('date', o.createdAt) as day,
             coalesce(sum(o.total), 0)
      from Order o
      where o.createdAt >= :start and o.createdAt < :end
        and o.status in :statuses
      group by function('date', o.createdAt)
      order by day
      """)
  List<Object[]> revenueDailyBetween(@Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end,
      @Param("statuses") List<OrderStatus> statuses);

  // KPI: số đơn trong khoảng theo nhiều trạng thái
  @Query("""
      select count(o)
      from Order o
      where o.createdAt >= :start and o.createdAt < :end
        and o.status in :statuses
      """)
  long countOrdersBetween(@Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end,
      @Param("statuses") List<OrderStatus> statuses);

  // KPI: tổng số item bán ra trong khoảng theo nhiều trạng thái
  @Query("""
      select coalesce(sum(oi.quantity), 0)
      from OrderItem oi
      where oi.order.createdAt >= :start and oi.order.createdAt < :end
        and oi.order.status in :statuses
      """)
  long sumItemsSoldBetween(@Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end,
      @Param("statuses") List<OrderStatus> statuses);

  // TOP SP theo SL (nhiều trạng thái)
  @Query("""
      select new com.ductieng.dto.TopProductDto(
        p.id,
        p.name,
        SUM(oi.quantity),
        SUM(oi.unitPrice * cast(oi.quantity as bigdecimal))
      )
      from Order o
      join o.items oi
      join oi.product p
      where o.createdAt >= :start and o.createdAt < :end
        and o.status in :statuses
      group by p.id, p.name
      order by SUM(oi.quantity) desc
      """)
  List<TopProductDto> topProductsByQtyBetween(@Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end,
      @Param("statuses") List<OrderStatus> statuses,
      Pageable pageable);

  // TOP SP theo Doanh thu (nhiều trạng thái)
  @Query("""
      select new com.ductieng.dto.TopProductDto(
        p.id,
        p.name,
        SUM(oi.quantity),
        SUM(oi.unitPrice * cast(oi.quantity as bigdecimal))
      )
      from Order o
      join o.items oi
      join oi.product p
      where o.createdAt >= :start and o.createdAt < :end
        and o.status in :statuses
      group by p.id, p.name
      order by SUM(oi.unitPrice * cast(oi.quantity as bigdecimal)) desc
      """)
  List<TopProductDto> topProductsByRevenueBetween(@Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end,
      @Param("statuses") List<OrderStatus> statuses,
      Pageable pageable);

  // ==================== PHỤC VỤ "THỐNG KÊ CHI TIẾT THEO NGÀY"
  // ====================

  // Lấy toàn bộ đơn trong khoảng (kèm customer + items + product) theo nhiều
  // trạng thái
  @Query("""
      select distinct o
      from Order o
      left join fetch o.customer c
      left join fetch o.items oi
      left join fetch oi.product p
      where o.createdAt >= :start and o.createdAt < :end
        and o.status in :statuses
      order by o.createdAt desc, o.id desc
      """)
  List<Order> findWithItemsInDay(@Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end,
      @Param("statuses") List<OrderStatus> statuses);

  // (Tuỳ chọn) Tổng hợp theo sản phẩm trong ngày
  @Query("""
      select p.id, p.name,
             sum(oi.quantity) as qty,
             sum(oi.unitPrice * cast(oi.quantity as bigdecimal)) as revenue
      from OrderItem oi
      join oi.order o
      join oi.product p
      where o.createdAt >= :start and o.createdAt < :end
        and o.status in :statuses
      group by p.id, p.name
      order by p.name
      """)
  List<Object[]> productSummaryInDay(@Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end,
      @Param("statuses") List<OrderStatus> statuses);

  // ==================== HỖ TRỢ CHỈNH SỬA ĐỊA CHỈ GIAO HÀNG ====================

  // Bảo mật: tìm đơn theo id và đúng chủ sở hữu
  Optional<Order> findByIdAndCustomerId(Long id, Long customerId);

  // (Khuyên dùng) Bản fetch join để hiển thị lại trang chi tiết sau cập nhật
  @Query("""
        select distinct o from Order o
        left join fetch o.customer c
        left join fetch o.items i
        left join fetch i.product p
        where o.id = :id and c.id = :customerId
      """)
  Optional<Order> findByIdAndCustomerIdWithItems(@Param("id") Long id,
      @Param("customerId") Long customerId);

  // (Tuỳ chọn) Cập nhật "atomic" có kiểm tra trạng thái ngay trên DB
  @Modifying
  @Transactional
  @Query("""
        update Order o
        set o.recipientName = :name,
            o.recipientPhone = :phone,
            o.recipientAddress = :address
        where o.id = :id
          and o.customer.id = :customerId
          and o.status in :editableStatuses
      """)
  int updateShippingIfEditable(@Param("id") Long id,
      @Param("customerId") Long customerId,
      @Param("name") String name,
      @Param("phone") String phone,
      @Param("address") String address,
      @Param("editableStatuses") List<OrderStatus> editableStatuses);
}
