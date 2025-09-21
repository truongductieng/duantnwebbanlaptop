package com.bigkhoa.controller;

import com.bigkhoa.model.Review;
import com.bigkhoa.repository.ReviewRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/reviews")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReviewController {

    private final ReviewRepository reviewRepo;

    public AdminReviewController(ReviewRepository reviewRepo) {
        this.reviewRepo = reviewRepo;
    }

    /**
     * Danh sách review có phân trang + tìm kiếm (giữ "q" để HTML cũ fill lại ô search nếu có).
     */
    @GetMapping
    public String page(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "12") int size,
                       @RequestParam(value = "q", required = false) String q,
                       Model model) {

        page = Math.max(page, 0);
        size = Math.max(size, 1);
        PageRequest pr = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        String kw = (q == null) ? null : q.trim();
        boolean hasKw = (kw != null && !kw.isEmpty());

        Page<Review> p = hasKw
                ? reviewRepo.search(kw, pr)   // <-- dùng search khi có q
                : reviewRepo.findAll(pr);     // fallback: không có q thì lấy tất cả

        model.addAttribute("q", q);
        model.addAttribute("page", p);
        model.addAttribute("reviews", p.getContent());
        return "admin/reviews";
    }

    /**
     * Stub: Template cũ nếu còn gọi thêm bình luận thì báo đã gỡ bỏ (tránh 404).
     */
    @PostMapping("/{rid}/comments")
    public String addCommentStub(@PathVariable Long rid,
                                 @RequestParam String content,
                                 RedirectAttributes ra) {
        ra.addFlashAttribute("error", "Tính năng bình luận cho đánh giá đã được gỡ bỏ.");
        return "redirect:/admin/reviews";
    }

    /**
     * Stub: Template cũ nếu còn nút ẩn/xoá bình luận thì báo đã gỡ bỏ (tránh 404).
     */
    @PostMapping("/comments/{id}/delete")
    public String deleteCommentStub(@PathVariable Long id,
                                    RedirectAttributes ra) {
        ra.addFlashAttribute("error", "Tính năng bình luận cho đánh giá đã được gỡ bỏ.");
        return "redirect:/admin/reviews";
    }

    /**
     * Xoá review.
     */
    @PostMapping("/{id}/delete")
    @Transactional
    public String deleteReview(@PathVariable Long id, RedirectAttributes ra) {
        if (!reviewRepo.existsById(id)) {
            ra.addFlashAttribute("error", "Review #" + id + " không tồn tại.");
            return "redirect:/admin/reviews";
        }
        reviewRepo.deleteById(id);
        ra.addFlashAttribute("message", "Đã xóa đánh giá #" + id);
        return "redirect:/admin/reviews";
    }
}
