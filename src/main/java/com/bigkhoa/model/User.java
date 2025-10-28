package com.bigkhoa.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users", uniqueConstraints = {
    @UniqueConstraint(name = "uk_users_username", columnNames = "username"),
    @UniqueConstraint(name = "uk_users_email", columnNames = "email")
}, indexes = {
    @Index(name = "idx_users_created_at", columnList = "created_at")
})
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank(message = "Username không được để trống")
  @Pattern(regexp = "^[a-zA-Z0-9._-]{3,50}$", message = "Username 3-50 ký tự, chỉ gồm chữ, số, ., _ hoặc -")
  @Column(nullable = false, unique = true, length = 50)
  private String username;

  @NotBlank(message = "Email không được để trống")
  @Email(message = "Email không hợp lệ")
  @Column(nullable = false, unique = true, length = 100)
  private String email;

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  // Không dùng @NotBlank ở đây để tránh lỗi khi chỉnh sửa (form không gửi password).
  // Luồng kiểm tra bắt buộc mật khẩu khi tạo mới đã được xử lý ở UserServiceImpl.save()
  @Column(nullable = false, length = 100) // >= 60 cho bcrypt
  private String password;

  @NotBlank(message = "Số điện thoại không được để trống")
  @Pattern(regexp = "^[0-9+()\\-\\s]{9,30}$", message = "Số điện thoại không hợp lệ (9-30 ký tự, chỉ gồm số, +, -, khoảng trắng, (), )")
  @Column(nullable = false, length = 30)
  private String phone;

  @Column(nullable = false, length = 30)
  private String role; // hoặc enum nếu anh muốn chặt chẽ hơn

  @Column(name = "full_name", length = 255)
  private String fullName;

  @Column(name = "avatar_url", length = 512)
  private String avatarUrl;

  // --- Trạng thái tài khoản (khuyến nghị) ---
  @Column(nullable = false)
  private boolean enabled = true;

  @Column(nullable = false)
  private boolean locked = false;

  @Column(name = "email_verified", nullable = false)
  private boolean emailVerified = false;

  @Column(name = "failed_login_attempts", nullable = false)
  private int failedLoginAttempts = 0;

  // --- Timestamps (dùng Hibernate annotations cho gọn) ---
  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  public User() {
  }

  // --- Chuẩn hoá dữ liệu trước khi lưu/cập nhật ---
  @PrePersist
  @PreUpdate
  protected void normalize() {
    if (email != null)
      email = email.trim().toLowerCase();
    if (username != null)
      username = username.trim().toLowerCase();
    if (fullName != null)
      fullName = fullName.trim();
    if (phone != null)
      phone = phone.trim();
    // Đảm bảo role luôn có giá trị hợp lệ khi persist/update
    if (role == null || role.trim().isEmpty()) {
      role = "ROLE_USER";
    }
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public String getAvatarUrl() {
    return avatarUrl;
  }

  public void setAvatarUrl(String avatarUrl) {
    this.avatarUrl = avatarUrl;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public boolean isLocked() {
    return locked;
  }

  public void setLocked(boolean locked) {
    this.locked = locked;
  }

  public boolean isEmailVerified() {
    return emailVerified;
  }

  public void setEmailVerified(boolean emailVerified) {
    this.emailVerified = emailVerified;
  }

  public int getFailedLoginAttempts() {
    return failedLoginAttempts;
  }

  public void setFailedLoginAttempts(int failedLoginAttempts) {
    this.failedLoginAttempts = failedLoginAttempts;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  // Getters & Setters ...
  // (gợi ý: không toString() lộ password)
}
