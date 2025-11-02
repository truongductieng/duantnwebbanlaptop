package com.ductieng.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

@Entity
@Table(name = "discounts", uniqueConstraints = @UniqueConstraint(columnNames = "code"))
public class Discount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 50)
    private String code;

    @Min(1) @Max(100)
    @Column(nullable = false)
    private int percent;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    @Column(nullable = false)
    private boolean active = true;

    public Discount() {}
    public Discount(String code, int percent) {
        this.code = code; this.percent = percent;
    }

    // ===== Getters/Setters =====
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public int getPercent() { return percent; }
    public void setPercent(int percent) { this.percent = percent; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    // Boolean property pattern: dùng isActive() cho field boolean
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    // ❌ BỎ hẳn getter này (nếu để lại phải trả về 'active')
    // public boolean getActive() { return false; }
}
