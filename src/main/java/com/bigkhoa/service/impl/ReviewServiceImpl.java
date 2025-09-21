package com.bigkhoa.service.impl;

import com.bigkhoa.dto.RatingAgg;
import com.bigkhoa.model.Laptop;
import com.bigkhoa.model.Review;
import com.bigkhoa.model.User;
import com.bigkhoa.repository.ReviewRepository;
import com.bigkhoa.service.ReviewService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;

    public ReviewServiceImpl(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
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
        if (laptopIds == null || laptopIds.isEmpty()) return Collections.emptyMap();
        return reviewRepository.findAggByLaptopIds(new ArrayList<>(laptopIds))
                .stream()
                .collect(Collectors.toMap(RatingAgg::laptopId, Function.identity()));
    }
}
