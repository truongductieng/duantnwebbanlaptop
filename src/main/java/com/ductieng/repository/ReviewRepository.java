package com.ductieng.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ductieng.dto.RatingAgg;
import com.ductieng.model.Laptop;
import com.ductieng.model.Review;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    // Trung bình rating (ép kiểu Double cho chắc)
    @Query("select coalesce(avg(r.rating), 0.0) from Review r where r.laptop.id = :laptopId")
    Double averageRatingByLaptopId(@Param("laptopId") Long laptopId);

    // Tổng số review
    @Query("select count(r) from Review r where r.laptop.id = :laptopId")
    long countByLaptopId(@Param("laptopId") Long laptopId);

    // Phân trang tất cả reviews, load kèm user & laptop để tránh N+1
    @EntityGraph(attributePaths = {"user", "laptop"})
    Page<Review> findAll(Pageable pageable);

    // Dùng ở service hiện tại
    @EntityGraph(attributePaths = {"user", "laptop"})
    List<Review> findByLaptop(Laptop laptop);

    // Tuỳ chọn: lấy mới nhất trước
    @EntityGraph(attributePaths = {"user", "laptop"})
    List<Review> findByLaptopIdOrderByCreatedAtDesc(Long laptopId);

    // Tuỳ chọn: chặn user review trùng
    boolean existsByLaptopIdAndUserId(Long laptopId, Long userId);

    // =========================
    // TÌM KIẾM (PHÂN TRANG)
    // =========================
    // Tìm theo từ khóa trên: tên laptop, tên/username/email user, hoặc nội dung bình luận (không phân biệt hoa/thường)
    @EntityGraph(attributePaths = {"user", "laptop"})
    @Query("""
        select r from Review r
          join r.user u
          join r.laptop l
        where (:kw is null or :kw = ''
           or lower(u.fullName) like lower(concat('%', :kw, '%'))
           or lower(u.username) like lower(concat('%', :kw, '%'))
           or lower(u.email)    like lower(concat('%', :kw, '%'))
           or lower(l.name)     like lower(concat('%', :kw, '%'))
           or lower(r.comment)  like lower(concat('%', :kw, '%'))
        )
        """)
    Page<Review> search(@Param("kw") String kw, Pageable pageable);

    // =========================
    // Gộp trung bình + số lượng theo danh sách laptop (dùng cho trang chủ)
    // =========================
    @Query("""
        select new com.ductieng.dto.RatingAgg(r.laptop.id, avg(r.rating), count(r))
        from Review r
        where r.laptop.id in :ids
        group by r.laptop.id
        """)
    List<RatingAgg> findAggByLaptopIds(@Param("ids") List<Long> ids);
}

