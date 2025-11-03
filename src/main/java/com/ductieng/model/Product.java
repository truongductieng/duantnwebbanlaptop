package com.ductieng.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

@Entity
@Table(name = "laptops")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Tên sản phẩm không được để trống")
    private String name;

    @NotBlank(message = "Hãng không được để trống")
    private String brand;

    @DecimalMin(value = "0.0", inclusive = true, message = "Giá phải lớn hơn hoặc bằng 0")
    private Double price;

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 0, message = "Số lượng phải lớn hơn hoặc bằng 0")
    @Column(name = "quantity", nullable = false)
    private Integer quantity = 0;

    // ====== QUAN HỆ DANH MỤC ======
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false) // cột FK trong bảng laptops
    private Category category; // <-- thêm field này

    // ===== 5 ảnh =====
    @Lob @Column(columnDefinition = "LONGBLOB") private byte[] image1;
    @Lob @Column(columnDefinition = "LONGBLOB") private byte[] image2;
    @Lob @Column(columnDefinition = "LONGBLOB") private byte[] image3;
    @Lob @Column(columnDefinition = "LONGBLOB") private byte[] image4;
    @Lob @Column(columnDefinition = "LONGBLOB") private byte[] image5;

    // Ảnh đại diện (1..5)
    @Column(name = "featured_index")
    private Integer featuredIndex = 1;

    // Cấu hình hiển thị
    @Lob
    @Column(columnDefinition = "TEXT")
    private String configuration;

    @Transient
    private MultipartFile[] imageFiles;

    public Product() {}

    // ===== Getters / Setters =====
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public byte[] getImage1() { return image1; }
    public void setImage1(byte[] image1) { this.image1 = image1; }
    public byte[] getImage2() { return image2; }
    public void setImage2(byte[] image2) { this.image2 = image2; }
    public byte[] getImage3() { return image3; }
    public void setImage3(byte[] image3) { this.image3 = image3; }
    public byte[] getImage4() { return image4; }
    public void setImage4(byte[] image4) { this.image4 = image4; }
    public byte[] getImage5() { return image5; }
    public void setImage5(byte[] image5) { this.image5 = image5; }

    public Integer getFeaturedIndex() { return featuredIndex; }
    public void setFeaturedIndex(Integer featuredIndex) { this.featuredIndex = featuredIndex; }

    public String getConfiguration() { return configuration; }
    public void setConfiguration(String configuration) { this.configuration = configuration; }

    public MultipartFile[] getImageFiles() { return imageFiles; }
    public void setImageFiles(MultipartFile[] imageFiles) { this.imageFiles = imageFiles; }
}
