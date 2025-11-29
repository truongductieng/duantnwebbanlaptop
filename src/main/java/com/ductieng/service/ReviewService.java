package com.ductieng.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.ductieng.dto.RatingAgg;
import com.ductieng.model.Laptop;
import com.ductieng.model.Review;
import com.ductieng.model.User;

public interface ReviewService {

    Review save(Review review);

    List<Review> findByLaptop(Laptop laptop);

    double averageRating(Laptop laptop);

    /**
     * Tạo review mới cho 1 laptop bởi 1 user, tự động clamp rating [1..5] và set
     * createdAt.
     */
    Review addReview(Laptop laptop, User user, int rating, String comment);

    /** Lấy trung bình + số lượng review theo lô laptopIds, trả về Map theo id. */
    Map<Long, RatingAgg> ratingAgg(Collection<Long> laptopIds);

    /**
     * Kiểm tra xem user đã mua laptop này với đơn hàng trạng thái DELIVERED hay
     * chưa.
     * 
     * @return true nếu user đã mua và đơn hàng đã giao, false nếu chưa
     */
    boolean hasUserPurchasedProduct(User user, Laptop laptop);
}
