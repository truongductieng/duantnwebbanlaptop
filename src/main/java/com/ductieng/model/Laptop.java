package com.ductieng.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Entity
@Table(name = "laptops")
public class Laptop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String brand;

    @Column(columnDefinition = "TEXT")
    private String configuration;

    private Double price;
    private Integer quantity;

    @Column(name = "featured_index")
    private Integer featuredIndex;

    // Ảnh dùng ở danh sách (nếu có) – đường dẫn tĩnh
    @Column(name = "image_url", length = 255)
    private String imageUrl;

    // 5 slot ảnh chi tiết – NHỊ PHÂN (ép LONGBLOB để không bị tinyblob)
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "image1", columnDefinition = "LONGBLOB")
    private byte[] image1;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "image2", columnDefinition = "LONGBLOB")
    private byte[] image2;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "image3", columnDefinition = "LONGBLOB")
    private byte[] image3;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "image4", columnDefinition = "LONGBLOB")
    private byte[] image4;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "image5", columnDefinition = "LONGBLOB")
    private byte[] image5;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "category_id", foreignKey = @ForeignKey(name = "fk_laptops_category"))
    private Category category;

    // ====== TRANSIENT: danh sách ảnh để UI build carousel ======
    @Transient
    private List<LaptopImage> images = new ArrayList<>();

    // ===== getters/setters =====
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getConfiguration() { return configuration; }
    public void setConfiguration(String configuration) { this.configuration = configuration; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Integer getFeaturedIndex() { return featuredIndex; }
    public void setFeaturedIndex(Integer featuredIndex) { this.featuredIndex = featuredIndex; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

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

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    // ===== Helpers cho UI/filter =====
    @Transient
    public List<LaptopImage> getImages() { return images; }
    public void setImages(List<LaptopImage> images) {
        this.images = (images != null) ? images : new ArrayList<>();
    }

    @Transient
    public boolean hasImage(int index) {
        byte[] d = switch (index) {
            case 1 -> image1;
            case 2 -> image2;
            case 3 -> image3;
            case 4 -> image4;
            case 5 -> image5;
            default -> null;
        };
        return d != null && d.length > 0;
    }

    /** Trả URL ảnh theo index nếu có, fallback imageUrl cho index 1 */
    @Transient
    public String getImageByIndex(int index) {
        if (getId() != null && hasImage(index)) {
            return "/product/" + getId() + "/image/" + index;
        }
        if (index == 1 && imageUrl != null && !imageUrl.isBlank()) return imageUrl;
        return null;
    }

    /** Stub RAM cho phần lọc; cố gắng parse từ configuration, không được thì trả 0. */
    @Transient
    public int getRam() {
        try {
            if (configuration != null) {
                String s = configuration.toUpperCase(Locale.ROOT);
                int i = s.indexOf("GB");
                if (i > 0) {
                    String digits = s.substring(Math.max(0, i - 3), i).replaceAll("[^0-9]", "");
                    if (!digits.isBlank()) return Integer.parseInt(digits);
                }
            }
        } catch (Exception ignore) {}
        return 0;
    }

    /** Stub CPU cho phần lọc; parse chuỗi nổi bật từ configuration (Intel/AMD/Apple...). */
    @Transient
    public String getCpu() {
        if (configuration == null || configuration.isBlank()) return null;
        String conf = configuration;

        try {
            Pattern[] patterns = new Pattern[] {
                Pattern.compile("(intel)\\s*core\\s*i[3-9](?:[-\\s]*\\w+)*", Pattern.CASE_INSENSITIVE),
                Pattern.compile("(amd)\\s*ryzen\\s*\\d{1,2}(?:[-\\s]*\\w+)*", Pattern.CASE_INSENSITIVE),
                Pattern.compile("(apple)?\\s*m[1-9](?:\\s*(pro|max|ultra))?", Pattern.CASE_INSENSITIVE)
            };

            for (Pattern p : patterns) {
                Matcher m = p.matcher(conf);
                if (m.find()) return m.group().trim().replaceAll("\\s+", " ");
            }

            int idx = conf.toLowerCase(Locale.ROOT).indexOf("cpu");
            if (idx >= 0) {
                String tail = conf.substring(idx + 3).replaceFirst("^[:\\s-]*", "");
                int cut = tail.indexOf(',');
                if (cut < 0) cut = tail.indexOf('\n');
                if (cut >= 0) tail = tail.substring(0, cut);
                tail = tail.trim();
                if (!tail.isBlank()) return tail;
            }
        } catch (Exception ignore) {}
        return null;
    }
}
