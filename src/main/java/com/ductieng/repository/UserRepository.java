package com.ductieng.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ductieng.model.User;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    User findByUsername(String username);

    // Tìm theo email, không phân biệt hoa-thường
    Optional<User> findByEmailIgnoreCase(String email);

    // Các check tồn tại
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByUsernameIgnoreCase(String username);
    boolean existsByEmailIgnoreCase(String email);

    // ✅ Đếm khách mới trong khoảng thời gian (native SQL)
    // Lưu ý: bảng 'users' và cột 'created_at' phải tồn tại trong DB.
    @Query(value = """
            SELECT COUNT(*) 
            FROM users 
            WHERE created_at >= :start AND created_at < :end
            """, nativeQuery = true)
    long countNewUsersBetween(@Param("start") LocalDateTime start,
                              @Param("end")   LocalDateTime end);
}
