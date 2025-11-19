package com.ductieng.service.impl;

import com.ductieng.model.Brand;
import com.ductieng.repository.BrandRepository;
import com.ductieng.repository.LaptopRepository;
import com.ductieng.service.BrandService;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;
    private final LaptopRepository laptopRepository;

    public BrandServiceImpl(BrandRepository brandRepository, LaptopRepository laptopRepository) {
        this.brandRepository = brandRepository;
        this.laptopRepository = laptopRepository;
    }

    @Override
    public List<Brand> getAllBrands() {
        return brandRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    @Override
    public Optional<Brand> findById(Long id) {
        return brandRepository.findById(id);
    }

    @Transactional
    @Override
    public Brand save(Brand brand) {
        return brandRepository.save(brand);
    }

    @Transactional
    @Override
    public void deleteById(Long id) {
        brandRepository.deleteById(id);
    }

    @Override
    public boolean existsByName(String name) {
        return brandRepository.existsByNameIgnoreCase(name);
    }

    @Override
    public boolean existsByNameAndNotId(String name, Long id) {
        Optional<Brand> existing = brandRepository.findByNameIgnoreCase(name);
        return existing.isPresent() && !existing.get().getId().equals(id);
    }

    @Override
    public long countProductsByBrand(String brandName) {
        return laptopRepository.countByBrandIgnoreCase(brandName);
    }
}
