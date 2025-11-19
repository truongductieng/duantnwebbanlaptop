package com.ductieng.service;

import com.ductieng.model.Brand;

import java.util.List;
import java.util.Optional;

public interface BrandService {

    /**
     * Lấy tất cả brands, sắp xếp theo tên
     */
    List<Brand> getAllBrands();

    /**
     * Tìm brand theo ID
     */
    Optional<Brand> findById(Long id);

    /**
     * Tạo hoặc cập nhật brand
     */
    Brand save(Brand brand);

    /**
     * Xóa brand theo ID
     */
    void deleteById(Long id);

    /**
     * Kiểm tra brand có tồn tại theo tên
     */
    boolean existsByName(String name);

    /**
     * Kiểm tra brand có tồn tại theo tên (trừ ID hiện tại - dùng khi update)
     */
    boolean existsByNameAndNotId(String name, Long id);

    /**
     * Đếm số sản phẩm dùng brand này
     */
    long countProductsByBrand(String brandName);
}
