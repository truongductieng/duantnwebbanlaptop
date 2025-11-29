package com.ductieng.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ductieng.dto.RatingAgg;
import com.ductieng.model.Laptop;
import com.ductieng.model.OrderStatus;
import com.ductieng.model.Review;
import com.ductieng.model.User;
import com.ductieng.repository.OrderRepository;
import com.ductieng.repository.ReviewRepository;
import com.ductieng.service.ReviewService;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;

    public ReviewServiceImpl(ReviewRepository reviewRepository, OrderRepository orderRepository) {
        this.reviewRepository = reviewRepository;
        this.orderRepository = orderRepository;
    }

    @Override
    public Review save(Review review) {
        return reviewRepository.save(review);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Review> findByLaptop(Laptop laptop) {
        return reviewRepository.findByLaptop(laptop);
    }

    @Override
    @Transactional(readOnly = true)
    public double averageRating(Laptop laptop) {
        Double avg = reviewRepository.averageRatingByLaptopId(laptop.getId());
        return avg == null ? 0.0 : avg;
    }

    @Override
    @Transactional
    public Review addReview(Laptop laptop, User user, int rating, String comment) {
        Review r = new Review();
        r.setLaptop(laptop);
        r.setUser(user);
        r.setRating(Math.max(1, Math.min(5, rating)));
        r.setComment(comment);
        r.setCreatedAt(LocalDateTime.now());
        return reviewRepository.save(r);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, RatingAgg> ratingAgg(Collection<Long> laptopIds) {
        if (laptopIds == null || laptopIds.isEmpty())
            return Collections.emptyMap();
        return reviewRepository.findAggByLaptopIds(new ArrayList<>(laptopIds))
                .stream()
                .collect(Collectors.toMap(RatingAgg::laptopId, Function.identity()));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasUserPurchasedProduct(User user, Laptop laptop) {
        if (user == null || laptop == null)
            return false;

        // Tìm các đơn hàng của user có trạng thái DELIVERED
        return orderRepository.findByCustomerAndStatus(user, OrderStatus.DELIVERED)
                .stream()
                .anyMatch(order -> order.getItems().stream()
                        .anyMatch(item -> item.getProduct() != null &&
                                item.getProduct().getId().equals(laptop.getId())));
    }
}
