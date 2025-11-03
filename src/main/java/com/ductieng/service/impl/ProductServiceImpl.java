package com.ductieng.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ductieng.model.Product;
import com.ductieng.repository.ProductRepository;
import com.ductieng.service.ProductService;

import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {
    private final ProductRepository repo;

    @Autowired
    public ProductServiceImpl(ProductRepository repo) {
        this.repo = repo;
    }

    @Override
    public List<Product> findAll() {
        return this.repo.findAll();
    }

    @Override
    public Product findById(Long id) {
        return repo.findById(id)
                   .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Product với id=" + id));
    }

    @Override
    public Product save(Product product) {
    	
        return repo.save(product);
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
    }
}
