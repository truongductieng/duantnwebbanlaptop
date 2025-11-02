package com.ductieng.model;

// Plain POJO để giữ tương thích compile; KHÔNG phải Entity nữa
public class LaptopImage {

    private Long id;
    private String url;
    private Laptop laptop;

    public LaptopImage() {}

    public LaptopImage(Laptop laptop, String url) {
        this.laptop = laptop;
        this.url = url;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public Laptop getLaptop() { return laptop; }
    public void setLaptop(Laptop laptop) { this.laptop = laptop; }
}
