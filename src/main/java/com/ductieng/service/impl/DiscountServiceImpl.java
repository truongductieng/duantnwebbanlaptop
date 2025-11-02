package com.ductieng.service.impl;

import org.springframework.stereotype.Service;

import com.ductieng.model.Discount;
import com.ductieng.repository.DiscountRepository;
import com.ductieng.service.DiscountService;

import java.time.LocalDate;

@Service
public class DiscountServiceImpl implements DiscountService {
    private final DiscountRepository repo;

    public DiscountServiceImpl(DiscountRepository repo) {
        this.repo = repo;
    }

    @Override
    public Integer getDiscountPercent(String code) {
        if (code == null || code.isBlank()) return null;
        return repo.findByCodeIgnoreCase(code.trim())
                .filter(this::isValid)
                .map(Discount::getPercent)
                .orElse(null);
    }

    private boolean isValid(Discount d) {
        if (!d.isActive()) return false;
        LocalDate today = LocalDate.now();
        if (d.getStartDate() != null && d.getStartDate().isAfter(today)) return false;
        if (d.getEndDate() != null && d.getEndDate().isBefore(today)) return false;
        return true;
    }
}
