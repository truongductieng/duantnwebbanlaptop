package com.ductieng.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "announcements")
public class Announcement {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 500)
  private String message;

  @Column(nullable = false)
  private boolean enabled = true;

  @Column(length = 20)
  private String variant = "INFO"; // INFO | SUCCESS | WARNING | DANGER

  @Column(length = 20)
  private String position = "TOP"; // TOP | BOTTOM

  @Column(length = 255)
  private String imageUrl; // public URL

  private LocalDateTime updatedAt;

  @PrePersist
  @PreUpdate
  private void touch() {
    this.updatedAt = LocalDateTime.now();
  }

  // === getters/setters ===
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getVariant() {
    return variant;
  }

  public void setVariant(String variant) {
    this.variant = variant;
  }

  public String getPosition() {
    return position;
  }

  public void setPosition(String position) {
    this.position = position;
  }

  /**
   * Trả về URL ảnh cho UI. Nếu DB lưu chỉ tên file, thêm prefix "/images/".
   * Nếu trường đã có URL đầy đủ, giữ nguyên.
   */
  public String getImageUrl() {
    if (imageUrl == null || imageUrl.isBlank())
      return imageUrl;
    String v = imageUrl.trim();
    if (v.startsWith("/") || v.startsWith("http://") || v.startsWith("https://"))
      return v;
    return "/images/" + v;
  }

  /**
   * Lưu vào DB chỉ tên file.
   * Nếu truyền vào "/images/abc.png" thì chỉ lưu "abc.png".
   */
  public void setImageUrl(String imageUrl) {
    if (imageUrl == null) {
      this.imageUrl = null;
      return;
    }
    String v = imageUrl.trim();
    v = v.replace('\\', '/');
    if (v.contains("/images/")) {
      int i = v.lastIndexOf('/');
      if (i >= 0 && i < v.length() - 1)
        v = v.substring(i + 1);
    }
    this.imageUrl = v;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  // ❗ Tuyệt đối KHÔNG để method orElse(...) trong entity
  // (nó làm che mất Optional#orElse ở controller)
}
