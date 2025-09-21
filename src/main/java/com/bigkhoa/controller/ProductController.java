package com.bigkhoa.controller;

import com.bigkhoa.model.Laptop;
import com.bigkhoa.model.Review;
import com.bigkhoa.model.User;
import com.bigkhoa.service.CartService;
import com.bigkhoa.service.LaptopService;
import com.bigkhoa.service.ProductService;
import com.bigkhoa.service.ReviewService;
import com.bigkhoa.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Locale;
import java.util.NoSuchElementException;

@Controller
@RequestMapping("/product")
public class ProductController {

    private final LaptopService laptopService;
    private final CartService cartService;
    private final ReviewService reviewService;
    private final UserService userService;
    private final ProductService productService;

    public ProductController(LaptopService laptopService,
                             CartService cartService,
                             ReviewService reviewService,
                             UserService userService,
                             ProductService productService) {
        this.laptopService = laptopService;
        this.cartService = cartService;
        this.reviewService = reviewService;
        this.userService = userService;
        this.productService = productService;
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model m) {
        Laptop lap;
        try {
            lap = laptopService.findById(id);
        } catch (NoSuchElementException | IllegalArgumentException ex) {
            return "redirect:/?notfound=1";
        }

        // Build carousel từ 5 slot ảnh chi tiết -> /product/{id}/image/{i}
        try {
            var imgs = new java.util.ArrayList<com.bigkhoa.model.LaptopImage>();
            for (int i = 1; i <= 5; i++) {
                if (lap.hasImage(i)) {
                    imgs.add(new com.bigkhoa.model.LaptopImage(lap, "/product/" + id + "/image/" + i));
                }
            }
            lap.setImages(imgs);
            if ((lap.getImageUrl() == null || lap.getImageUrl().isBlank()) && !imgs.isEmpty()) {
                lap.setImageUrl(imgs.get(0).getUrl());
            }
        } catch (Exception ignore) {}

        m.addAttribute("laptop", lap);
        m.addAttribute("reviews", reviewService.findByLaptop(lap));
        m.addAttribute("avgRating", reviewService.averageRating(lap));
        return "product";
    }

    @PostMapping("/{id}/add-to-cart")
    public String addToCart(@PathVariable Long id,
                            @RequestParam(defaultValue = "1") int quantity,
                            Authentication auth) {
        try {
            if (quantity < 1) quantity = 1;
            cartService.addToCart(id, quantity, auth);
            return "redirect:/cart?added";
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Laptop không tồn tại");
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thêm được vào giỏ");
        }
    }

    // === GỬI REVIEW: AJAX -> trả fragment; bình thường -> redirect ===
    @PostMapping(value = "/{id}/review", produces = MediaType.TEXT_HTML_VALUE)
    public String addReview(@PathVariable Long id,
                            @RequestParam(name = "rating", defaultValue = "5") int rating,
                            @RequestParam(name = "comment", required = false) String comment,
                            Authentication auth,
                            HttpServletRequest request,
                            RedirectAttributes ra,
                            Model model) {
        boolean isAjax = "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"));

        if (auth == null || !auth.isAuthenticated()) {
            if (isAjax) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bạn cần đăng nhập để đánh giá");
            }
            ra.addFlashAttribute("error", "Vui lòng đăng nhập để đánh giá.");
            return "redirect:/login";
        }

        try {
            Laptop lap = laptopService.findById(id);
            User user = userService.findByUsername(auth.getName());
            if (user == null) {
                if (isAjax) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không tìm thấy tài khoản");
                ra.addFlashAttribute("error", "Không tìm thấy tài khoản.");
                return "redirect:/login";
            }

            Review saved = reviewService.addReview(lap, user, rating, comment);

            if (isAjax) {
                // Trả HTML của 1 review item để JS prepend vào danh sách
                model.addAttribute("r", saved);
                return "fragments/review-item :: item";
            } else {
                ra.addFlashAttribute("message", "Cảm ơn bạn đã đánh giá sản phẩm!");
                return "redirect:/product/" + id;
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            if (isAjax) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gửi đánh giá thất bại");
            ra.addFlashAttribute("error", "Gửi đánh giá thất bại.");
            return "redirect:/product/" + id;
        }
    }

    // === Trả trung bình rating dạng text/plain để JS cập nhật ===
    @GetMapping(value = "/{id}/avg", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String avgRating(@PathVariable Long id) {
        Laptop lap = laptopService.findById(id);
        double avg = reviewService.averageRating(lap);
        return String.format(Locale.US, "%.1f", avg);
    }
}
