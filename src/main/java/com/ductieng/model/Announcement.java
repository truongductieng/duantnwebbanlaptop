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
  private String variant = "INFO";   // INFO | SUCCESS | WARNING | DANGER

  @Column(length = 20)
  private String position = "TOP";   // TOP | BOTTOM

  @Column(length = 255)
  private String imageUrl;           // public URL

  private LocalDateTime updatedAt;

  @PrePersist
  @PreUpdate
  private void touch() {
    this.updatedAt = LocalDateTime.now();
  }

  // === getters/setters ===
  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }

  public String getMessage() { return message; }
  public void setMessage(String message) { this.message = message; }

  public boolean isEnabled() { return enabled; }
  public void setEnabled(boolean enabled) { this.enabled = enabled; }

  public String getVariant() { return variant; }
  public void setVariant(String variant) { this.variant = variant; }

  public String getPosition() { return position; }
  public void setPosition(String position) { this.position = position; }

  public String getImageUrl() { return imageUrl; }
  public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

  // ❗ Tuyệt đối KHÔNG để method orElse(...) trong entity
  // (nó làm che mất Optional#orElse ở controller)
}
