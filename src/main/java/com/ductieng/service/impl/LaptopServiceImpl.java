package com.ductieng.service.impl;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ductieng.model.Laptop;
import com.ductieng.repository.LaptopRepository;
import com.ductieng.service.BrandService;
import com.ductieng.service.LaptopService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class LaptopServiceImpl implements LaptopService {

    private final LaptopRepository laptopRepo;
    private final BrandService brandService;

    public LaptopServiceImpl(LaptopRepository laptopRepo, BrandService brandService) {
        this.laptopRepo = laptopRepo;
        this.brandService = brandService;
    }

    // ===== Public APIs =====

    @Override
    public Page<Laptop> search(String brand, int page, int size, String sort) {
        Pageable pageable = buildPageable(page, size, sort);
        String kw = sanitize(brand);
        return kw.isEmpty()
                ? laptopRepo.findAll(pageable)
                : laptopRepo.findByBrandContainingIgnoreCase(kw, pageable);
    }

    @Override
    public Page<Laptop> searchByCategoryKey(String categoryKey, String brand, int page, int size, String sort) {
        Long cid = mapKeyToCategoryId(categoryKey);
        return searchByCategoryId(cid, brand, page, size, sort);
    }

    @Override
    public Page<Laptop> searchByCategoryId(Long categoryId, String brand, int page, int size, String sort) {
        // Nếu không có category → fallback về search all
        if (categoryId == null)
            return search(brand, page, size, sort);

        Pageable pageable = buildPageable(page, size, sort);
        String kw = sanitize(brand);

        return kw.isEmpty()
                ? laptopRepo.findByCategory_Id(categoryId, pageable)
                : laptopRepo.findByCategory_IdAndBrandContainingIgnoreCase(categoryId, kw, pageable);
    }

    @Override
    public Laptop findById(Long id) {
        return laptopRepo.findById(id).orElse(null);
    }

    @Transactional
    @Override
    public Laptop save(Laptop laptop) {
        return laptopRepo.save(laptop);
    }

    // ===== Convenience (không phân trang) =====

    @Override
    public List<Laptop> getOfficeLaptops() {
        Long cid = mapKeyToCategoryId("office");
        return laptopRepo.findByCategory_Id(cid);
    }

    @Override
    public List<Laptop> getGamingLaptops() {
        Long cid = mapKeyToCategoryId("gaming");
        return laptopRepo.findByCategory_Id(cid);
    }

    @Override
    public List<Laptop> getStudyLaptops() {
        Long cid = mapKeyToCategoryId("study");
        return laptopRepo.findByCategory_Id(cid);
    }

    @Override
    public List<String> getAllBrands() {
        // Lấy tất cả brands từ bảng Brand, không chỉ từ products
        // Như vậy brand mới thêm sẽ hiển thị ngay cả khi không có sản phẩm
        return brandService.getAllBrands()
                .stream()
                .map(b -> b.getName())
                .collect(Collectors.toList());
    }

    // ===== Helpers =====

    private Pageable buildPageable(int page, int size, String sort) {
        int p = Math.max(page, 0);
        int s = Math.max(size, 1);

        Sort order = switch (sort == null ? "" : sort) {
            case "new" -> Sort.by(Sort.Direction.DESC, "id");
            case "priceAsc" -> Sort.by(Sort.Direction.ASC, "price");
            case "priceDesc" -> Sort.by(Sort.Direction.DESC, "price");
            default -> Sort.by(Sort.Direction.ASC, "id");
        };
        return PageRequest.of(p, s, order);
        // NOTE: nếu muốn secondary sort ổn định:
        // return PageRequest.of(p, s, order.and(Sort.by("id").ascending()));
    }

    private String sanitize(String text) {
        return text == null ? "" : text.trim();
    }

    /**
     * Map "office"/"study"/"gaming" → ID trong bảng categories.
     * Đổi lại các con số này theo seed của DB anh đang dùng.
     */
    private Long mapKeyToCategoryId(String key) {
        String k = (key == null ? "all" : key.trim().toLowerCase());
        return switch (k) {
            case "office" -> 1L; // Laptop văn phòng
            case "study" -> 2L; // Laptop sinh viên
            case "gaming" -> 3L; // Laptop gaming
            default -> null; // all
        };
    }
}
