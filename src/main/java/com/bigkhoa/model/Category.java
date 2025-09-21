package com.bigkhoa.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categories") // CHỐT chỉ dùng bảng này
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, unique=true, length=120)
    private String name;

    @OneToMany(mappedBy = "category")
    private List<Laptop> laptops = new ArrayList<>();

    // ===== GET/SET =====
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<Laptop> getLaptops() { return laptops; }
    public void setLaptops(List<Laptop> laptops) { this.laptops = laptops; }
}
