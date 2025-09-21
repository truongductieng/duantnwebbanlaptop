package com.bigkhoa.repository;

import java.util.List;

public interface LaptopImageRepository {
    List<String> findByLaptopId(Long laptopId);
}