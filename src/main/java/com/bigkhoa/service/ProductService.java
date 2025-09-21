package com.bigkhoa.service;

import com.bigkhoa.model.Product;

import java.util.List;

import org.springframework.ui.Model;

public interface ProductService {
    List<Product> findAll();
    Product    findById(Long id);
    Product    save(Product product);
    void       deleteById(Long id);

}
