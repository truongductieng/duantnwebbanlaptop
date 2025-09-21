package com.bigkhoa.service;

import com.bigkhoa.dto.RatingAgg;
import com.bigkhoa.model.Laptop;
import com.bigkhoa.model.Review;
import com.bigkhoa.model.User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ReviewService {

    Review save(Review review);

    List<Review> findByLaptop(Laptop laptop);

    double averageRating(Laptop laptop);

    /**
     * Tạo review mới cho 1 laptop bởi 1 user, tự động clamp rating [1..5] và set createdAt.
     */
    Review addReview(Laptop laptop, User user, int rating, String comment);

    /** Lấy trung bình + số lượng review theo lô laptopIds, trả về Map theo id. */
    Map<Long, RatingAgg> ratingAgg(Collection<Long> laptopIds);
}
