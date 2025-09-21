package com.bigkhoa.repository;

import com.bigkhoa.model.Discount;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DiscountRepository extends JpaRepository<Discount, Long> {

    Optional<Discount> findByCodeIgnoreCase(String code);

    // ====== Các method KHỚP với AdminController ======

    // Đếm mã active + valid tính theo CURRENT_DATE
    @Query("""
           SELECT COUNT(d) FROM Discount d 
           WHERE d.active = true
             AND (d.startDate IS NULL OR d.startDate <= CURRENT_DATE)
             AND (d.endDate   IS NULL OR d.endDate   >= CURRENT_DATE)
           """)
    long countActiveValid();

    // Đếm mã inactive hoặc hết hạn / chưa tới ngày hiệu lực
    @Query("""
           SELECT COUNT(d) FROM Discount d
           WHERE d.active = false
              OR (d.endDate IS NOT NULL AND d.endDate < CURRENT_DATE)
              OR (d.startDate IS NOT NULL AND d.startDate > CURRENT_DATE)
           """)
    long countInactiveOrExpired();

    // Lấy 5 mã mới nhất
    List<Discount> findTop5ByOrderByIdDesc();

    // ====== Tiện ích (giữ lại nếu anh muốn dùng ở chỗ khác) ======

    default List<Discount> findLatestDiscounts(int n) {
        return findAll(PageRequest.of(0, n, Sort.by(Sort.Direction.DESC, "id"))).getContent();
    }

    // Alias nếu anh đã dùng ở nơi khác:
    // (Không bắt buộc; có thể bỏ)
    @Deprecated
    default long countActiveDiscounts() { return countActiveValid(); }

    @Deprecated
    default long countInactiveDiscounts() { return countInactiveOrExpired(); }
}
