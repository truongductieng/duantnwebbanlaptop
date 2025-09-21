package com.bigkhoa.service;

import com.bigkhoa.model.Laptop;
import org.springframework.data.domain.Page;

import java.util.List;

public interface LaptopService {

    /**
     * Tìm & phân trang, có thể sort theo:
     *   - "new"       → id desc
     *   - "priceAsc"  → price asc
     *   - "priceDesc" → price desc
     */
    Page<Laptop> search(String brand, int page, int size, String sort);

    /**
     * Tìm theo key của tab cũ: all | office | study | gaming
     * (dùng khi controller truyền "category" dạng chuỗi).
     */
    Page<Laptop> searchByCategoryKey(String categoryKey, String brand, int page, int size, String sort);

    /**
     * Tìm theo ID danh mục trong DB (dùng khi controller truyền thẳng categoryId).
     */
    Page<Laptop> searchByCategoryId(Long categoryId, String brand, int page, int size, String sort);

    Laptop findById(Long id);

    Laptop save(Laptop laptop);

    // Tiện ích cho các block cũ (không phân trang)
    List<Laptop> getOfficeLaptops();
    List<Laptop> getGamingLaptops();
    List<Laptop> getStudyLaptops();
}
