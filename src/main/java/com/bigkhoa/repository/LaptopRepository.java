package com.bigkhoa.repository;

import com.bigkhoa.model.Laptop;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LaptopRepository extends JpaRepository<Laptop, Long> {

    // Tìm kiếm theo brand (phân trang)
    Page<Laptop> findByBrandContainingIgnoreCase(String brand, Pageable pageable);

    // Phân trang theo category thực trong DB
    Page<Laptop> findByCategory_Id(Long categoryId, Pageable pageable);

    // Lấy List theo category (dùng cho block “văn phòng / gaming / sinh viên” cũ)
    List<Laptop> findByCategory_Id(Long categoryId);

    // Phân trang theo category + keyword brand
    Page<Laptop> findByCategory_IdAndBrandContainingIgnoreCase(Long categoryId, String brand, Pageable pageable);

    // Phân loại theo giá (nếu nơi khác dùng)
    List<Laptop> findByPriceBetween(double min, double max);
    List<Laptop> findByPriceGreaterThan(double price);
    List<Laptop> findByPriceLessThan(double price);
}
