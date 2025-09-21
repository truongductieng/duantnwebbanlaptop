package com.bigkhoa.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 1 review thuộc 1 laptop
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "laptop_id", nullable = false)
    private Laptop laptop;

    // 1 review thuộc 1 user (customer)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private int rating;  // 1..5

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // ===== GET/SET =====
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Laptop getLaptop() { return laptop; }
    public void setLaptop(Laptop laptop) { this.laptop = laptop; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
