package com.bigkhoa.controller;

import com.bigkhoa.dto.RevenueDataDto;
import com.bigkhoa.dto.TopProductDto;
import com.bigkhoa.exception.EmailExistsException;
import com.bigkhoa.exception.UsernameExistsException;
import com.bigkhoa.model.*;
import com.bigkhoa.repository.CategoryRepository;
import com.bigkhoa.repository.DiscountRepository;
import com.bigkhoa.repository.OrderRepository;
import com.bigkhoa.repository.UserRepository;
import com.bigkhoa.service.LaptopService;
import com.bigkhoa.service.OrderService;
import com.bigkhoa.service.ProductService;
import com.bigkhoa.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity; // <== THÊM
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.sql.Date;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final ProductService productService;
    private final UserService userService;
    private final OrderService orderService;
    private final LaptopService laptopService;
    private final DiscountRepository discountRepo;
    private final CategoryRepository categoryRepo;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Autowired
    public AdminController(ProductService productService,
            UserService userService,
            OrderService orderService,
            LaptopService laptopService,
            DiscountRepository discountRepo,
            CategoryRepository categoryRepo,
            OrderRepository orderRepository,
            UserRepository userRepository) {
        this.productService = productService;
        this.userService = userService;
        this.orderService = orderService;
        this.laptopService = laptopService;
        this.discountRepo = discountRepo;
        this.categoryRepo = categoryRepo;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    // Ngăn hiển thị số mũ
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        NumberFormat nf = new DecimalFormat("0.############");
        nf.setGroupingUsed(false);
        binder.registerCustomEditor(Double.class, new CustomNumberEditor(Double.class, nf, true));
        binder.registerCustomEditor(double.class, new CustomNumberEditor(Double.class, nf, true));
    }

    @ModelAttribute("categories")
    public List<Category> categories() {
        return categoryRepo.findAll();
    }

    // ================== DASHBOARD ==================
    @GetMapping("/dashboard")
    public String dashboard(
            @RequestParam(value = "type", defaultValue = "daily") String type,
            @RequestParam(value = "start", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(value = "end", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(value = "startMonth", required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth startMonth,
            @RequestParam(value = "endMonth", required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth endMonth,
            Model model) {
        // Dữ liệu list
        List<Product> products = productService.findAll();
        List<User> users = userService.findAll();

        // Đơn PENDING để hiện "Đơn mới cần duyệt" & section Orders
        List<Order> pendingOrders = orderService.getByStatus(OrderStatus.PENDING);

        // Tổng theo trạng thái hoàn thành (KHÔNG tính COMPLETED)
        BigDecimal totalRevenue = orderService.getTotalRevenue(OrderStatus.DELIVERED);
        Long totalQuantity = orderService.getTotalQuantity(OrderStatus.DELIVERED);

        model.addAttribute("list", products);
        model.addAttribute("users", users);
        model.addAttribute("orders", pendingOrders);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("totalQuantity", totalQuantity);

        // ====== KPI hôm nay + 7 ngày (để tính hôm qua) ======
        ZoneId HCM = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalDate today = LocalDate.now(HCM);
        LocalDateTime startToday = today.atStartOfDay();
        LocalDateTime endToday = today.plusDays(1).atStartOfDay();

        // Status tính doanh thu (ĐÃ LOẠI COMPLETED)
        List<OrderStatus> revenueStatuses = List.of(
                OrderStatus.CONFIRMED,
                OrderStatus.SHIPPED,
                OrderStatus.DELIVERED);

        // Doanh thu hôm nay
        BigDecimal todayRevenue = orderRepository.sumTotalBetween(startToday, endToday, revenueStatuses);

        // 7 ngày gần nhất
        LocalDate startDay = today.minusDays(6);
        LocalDateTime start7 = startDay.atStartOfDay();
        List<Object[]> rows = orderRepository.revenueDailyBetween(start7, endToday, revenueStatuses);

        Map<LocalDate, BigDecimal> seriesMap = new LinkedHashMap<>();
        for (int i = 0; i < 7; i++) {
            seriesMap.put(startDay.plusDays(i), BigDecimal.ZERO);
        }
        for (Object[] r : rows) {
            LocalDate d = ((Date) r[0]).toLocalDate();
            BigDecimal amount = (BigDecimal) r[1];
            seriesMap.put(d, amount);
        }

        DateTimeFormatter labFmt = DateTimeFormatter.ofPattern("dd/MM");
        String labelsJson = seriesMap.keySet().stream()
                .map(d -> "\"" + d.format(labFmt) + "\"")
                .collect(Collectors.joining(",", "[", "]"));
        String seriesJson = seriesMap.values().stream()
                .map(BigDecimal::toString)
                .collect(Collectors.joining(",", "[", "]"));

        model.addAttribute("todayRevenue", todayRevenue);
        model.addAttribute("todayStr", today.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        model.addAttribute("labelsJson", labelsJson);
        model.addAttribute("seriesJson", seriesJson);

        // KPI khác
        long ordersToday = orderRepository.countOrdersBetween(startToday, endToday,
                Arrays.asList(OrderStatus.values()));

        long itemsSoldToday = orderRepository.sumItemsSoldBetween(startToday, endToday, revenueStatuses);
        long cancelledToday = orderRepository.countOrdersBetween(startToday, endToday, List.of(OrderStatus.CANCELED));

        long newUsersToday = 0L;
        try {
            newUsersToday = userRepository.countNewUsersBetween(startToday, endToday);
        } catch (Exception ignore) {
        }

        model.addAttribute("ordersToday", ordersToday);
        model.addAttribute("itemsSoldToday", itemsSoldToday);
        model.addAttribute("cancelledToday", cancelledToday);
        model.addAttribute("newUsersToday", newUsersToday);

        // TOP 7 ngày (ĐÃ LOẠI COMPLETED)
        Pageable top5 = PageRequest.of(0, 5);
        List<OrderStatus> topStatuses = revenueStatuses;
        List<TopProductDto> topQty7 = orderRepository.topProductsByQtyBetween(start7, endToday, topStatuses, top5);
        model.addAttribute("topQty7", topQty7);

        // Bộ lọc thống kê
        if ("daily".equalsIgnoreCase(type)) {
            LocalDate s = (start != null ? start : today.minusDays(7));
            LocalDate e = (end != null ? end : today);
            List<RevenueDataDto> data = orderService.getDailyRevenue(s, e);
            model.addAttribute("start", s);
            model.addAttribute("end", e);
            model.addAttribute("revenueData", data);
        } else {
            YearMonth thisMonth = YearMonth.now(HCM);
            YearMonth sm = (startMonth != null ? startMonth : thisMonth.minusMonths(5));
            YearMonth em = (endMonth != null ? endMonth : thisMonth);
            List<RevenueDataDto> data = orderService.getMonthlyRevenue(
                    sm.atDay(1), em.atEndOfMonth());
            model.addAttribute("startMonth", sm);
            model.addAttribute("endMonth", em);
            model.addAttribute("revenueData", data);
        }
        model.addAttribute("type", type);

        // Discount stats
        model.addAttribute("activeDiscounts", discountRepo.countActiveValid());
        model.addAttribute("inactiveDiscounts", discountRepo.countInactiveOrExpired());
        model.addAttribute("latestDiscounts", discountRepo.findTop5ByOrderByIdDesc());

        return "admin/dashboard";
    }

    // ===== AJAX: Dashboard metrics theo ngày =====
    @GetMapping("/api/dashboard/metrics")
    @ResponseBody
    public Metrics metricsByDate(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = start.plusDays(1);

        // Trạng thái được tính doanh thu / SP bán ra (ĐÃ LOẠI COMPLETED)
        List<OrderStatus> SOLD_OK = List.of(
                OrderStatus.CONFIRMED,
                OrderStatus.SHIPPED,
                OrderStatus.DELIVERED);

        // Số đơn: tính tất cả trạng thái (để thấy PENDING)
        List<OrderStatus> ALL = Arrays.asList(OrderStatus.values());

        BigDecimal revenue = orderRepository.sumTotalBetween(start, end, SOLD_OK);
        long ordersCount = orderRepository.countOrdersBetween(start, end, ALL);
        long soldItems = orderRepository.sumItemsSoldBetween(start, end, SOLD_OK);
        long canceledOrders = orderRepository.countOrdersBetween(start, end, List.of(OrderStatus.CANCELED));
        long newCustomers = 0L;
        try {
            newCustomers = userRepository.countNewUsersBetween(start, end);
        } catch (Exception ignore) {
        }

        String label = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        return new Metrics(label, revenue, ordersCount, soldItems, canceledOrders, newCustomers);
    }

    // DTO trả JSON
    static record Metrics(
            String dateLabel,
            BigDecimal revenue,
            long ordersCount,
            long soldItems,
            long canceledOrders,
            long newCustomers) {
    }

    // ================== ORDERS ==================
    @GetMapping("/orders/{id}")
    public String viewOrderDetail(@PathVariable("id") Long id,
            HttpServletRequest request,
            Model model,
            RedirectAttributes ra) {
        Order order = orderService.getById(id);
        if (order == null) {
            ra.addFlashAttribute("error", "Không tìm thấy đơn hàng #" + id);
            return "redirect:/admin/orders";
        }
        model.addAttribute("order", order);
        model.addAttribute("items", order.getItems());

        String referer = request.getHeader("Referer");
        model.addAttribute("backUrl", (referer != null && !referer.isBlank()) ? referer : "/admin/orders");
        return "order_detail";
    }

    @GetMapping("/orders")
    public String listOrders(@RequestParam(value = "status", required = false) OrderStatus status,
            @RequestParam(value = "orderId", required = false) Long orderId,
            HttpServletRequest request,
            Model model) {

        // LOẠI COMPLETED khỏi dropdown/filter UI
        List<OrderStatus> adminStatuses = Arrays.stream(OrderStatus.values())
                .filter(s -> s != OrderStatus.COMPLETED)
                .collect(Collectors.toList());
        model.addAttribute("allStatuses", adminStatuses);

        // Nếu nhập ID -> ưu tiên tìm theo ID, KHÔNG ném exception khi không có
        if (orderId != null) {
            Order found = null;
            try {
                found = orderRepository.findById(orderId).orElse(null);
            } catch (Exception ignore) {
                /* tránh 500 */ }

            List<Order> orders = (found != null) ? java.util.List.of(found) : java.util.List.of();
            model.addAttribute("orders", orders);
            model.addAttribute("selectedStatus", (status != null ? status : OrderStatus.PENDING)); // giữ UI
            model.addAttribute("searchOrderId", orderId); // bind lại input
            model.addAttribute("notFound", found == null); // hiện alert

            String referer = request.getHeader("Referer");
            model.addAttribute("backUrl", (referer != null && !referer.isBlank()) ? referer : "/admin/dashboard");
            return "admin/orders";
        }

        // Luồng cũ: lọc theo trạng thái
        OrderStatus st = (status != null ? status : OrderStatus.PENDING);
        List<Order> orders = orderService.getByStatus(st);

        model.addAttribute("orders", orders);
        model.addAttribute("selectedStatus", st);

        String referer = request.getHeader("Referer");
        model.addAttribute("backUrl", (referer != null && !referer.isBlank()) ? referer : "/admin/dashboard");
        return "admin/orders";
    }

    @PostMapping("/orders/{id}/status")
    public String changeOrderStatus(@PathVariable("id") Long id,
            @RequestParam("status") OrderStatus newStatus,
            RedirectAttributes ra) {
        try {
            // CHẶN cập nhật sang COMPLETED
            if (newStatus == OrderStatus.COMPLETED) {
                ra.addFlashAttribute("error", "Trạng thái ‘Hoàn tất’ đã bị vô hiệu hoá.");
                return "redirect:/admin/orders";
            }

            orderService.updateStatus(id, newStatus);
            ra.addFlashAttribute("success", "Đã cập nhật trạng thái đơn #" + id + " → " + newStatus.getLabel());
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Cập nhật trạng thái thất bại: " + e.getMessage());
        }
        return "redirect:/admin/orders";
    }

    @PostMapping("/orders/{id}/cancel")
    public String cancelOrderByAdmin(@PathVariable("id") Long id,
            @RequestParam(value = "reason", required = false) String reason,
            RedirectAttributes ra) {
        try {
            orderService.cancelOrderByAdmin(id, reason);
            ra.addFlashAttribute("success", "Đã hủy đơn #" + id + " (ADMIN).");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Hủy đơn thất bại: " + e.getMessage());
        }
        return "redirect:/admin/orders";
    }

    @PostMapping("/orders/{id}/delete")
    public String deleteOrder(@PathVariable Long id, RedirectAttributes ra) {
        try {
            orderService.deleteOrder(id);
            ra.addFlashAttribute("success", "Đã xóa đơn #" + id);
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Xóa đơn thất bại: " + e.getMessage());
        }
        return "redirect:/admin/orders";
    }

    // ======= API JSON cho AJAX (KHÔNG Điều hướng) =======
    @PostMapping("/api/orders/{id}/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> apiChangeOrderStatus(@PathVariable("id") Long id,
            @RequestParam("status") OrderStatus newStatus) {
        Map<String, Object> out = new HashMap<>();
        try {
            if (newStatus == OrderStatus.COMPLETED) {
                out.put("ok", false);
                out.put("error", "Trạng thái ‘Hoàn tất’ đã bị vô hiệu hoá.");
                return ResponseEntity.badRequest().body(out);
            }

            orderService.updateStatus(id, newStatus);

            // >>> THÊM: trả số lượng đơn PENDING còn lại để cập nhật badge
            long pendingCount = orderService.countByStatus(OrderStatus.PENDING);

            out.put("ok", true);
            out.put("id", id);
            out.put("newStatus", newStatus.name());
            out.put("message", "Updated");
            out.put("pendingCount", pendingCount); // <<<
            return ResponseEntity.ok(out);
        } catch (Exception e) {
            out.put("ok", false);
            out.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(out);
        }
    }

    @PostMapping("/api/orders/{id}/cancel")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> apiCancelOrder(@PathVariable("id") Long id,
            @RequestParam(value = "reason", required = false) String reason) {
        Map<String, Object> out = new HashMap<>();
        try {
            orderService.cancelOrderByAdmin(id, reason);
            out.put("ok", true);
            out.put("id", id);
            out.put("newStatus", OrderStatus.CANCELED.name());
            return ResponseEntity.ok(out);
        } catch (Exception e) {
            out.put("ok", false);
            out.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(out);
        }
    }
    // ================== /ORDERS ==================

    // ================== PRODUCT ==================
    @GetMapping("/product/new")
    public String createProductForm(Model model) {
        Product p = new Product();
        p.setFeaturedIndex(1);
        model.addAttribute("product", p);
        return "admin/product-form";
    }

    // >>> THÊM MỚI: FORM SỬA <<<
    @GetMapping({ "/product/{id}/edit", "/products/{id}/edit" })
    public String editProductForm(@PathVariable Long id,
            Model model,
            RedirectAttributes ra) {
        Product p = productService.findById(id);
        if (p == null) {
            ra.addFlashAttribute("error", "Không tìm thấy sản phẩm #" + id);
            return "redirect:/admin/dashboard";
        }
        model.addAttribute("product", p);
        return "admin/product-form";
    }

    @PostMapping("/product/save")
    public String saveProduct(
            @Valid @ModelAttribute("product") Product product,
            BindingResult result,
            @RequestParam(value = "imageFiles", required = false) List<MultipartFile> imageFiles,
            @RequestParam(value = "featuredIndex", required = false) Integer featuredIndex,
            @RequestParam(value = "action", required = false) String action, // <-- thêm
            RedirectAttributes ra,
            Model model) {

        if (result.hasErrors()) {
            model.addAttribute("product", product);
            return "admin/product-form";
        }

        try {
            Product existing = (product.getId() != null) ? productService.findById(product.getId()) : null;

            List<MultipartFile> files = (imageFiles != null) ? new ArrayList<>(imageFiles) : new ArrayList<>();
            while (files.size() < 5)
                files.add(null);

            for (int idx = 0; idx < 5; idx++) {
                MultipartFile file = files.get(idx);
                byte[] data = null;
                if (file != null && !file.isEmpty()) {
                    try {
                        data = file.getBytes();
                    } catch (IOException ignore) {
                    }
                }
                int slot = idx + 1;
                if (data != null && data.length > 0) {
                    switch (slot) {
                        case 1 -> product.setImage1(data);
                        case 2 -> product.setImage2(data);
                        case 3 -> product.setImage3(data);
                        case 4 -> product.setImage4(data);
                        case 5 -> product.setImage5(data);
                    }
                } else if (existing != null) {
                    switch (slot) {
                        case 1 -> product.setImage1(existing.getImage1());
                        case 2 -> product.setImage2(existing.getImage2());
                        case 3 -> product.setImage3(existing.getImage3());
                        case 4 -> product.setImage4(existing.getImage4());
                        case 5 -> product.setImage5(existing.getImage5());
                    }
                }
            }

            if (product.getCategory() != null && product.getCategory().getId() != null) {
                Long cid = product.getCategory().getId();
                Category cat = categoryRepo.findById(cid).orElse(null);
                if (cat == null) {
                    result.rejectValue("category", "invalid", "Danh mục không tồn tại");
                    model.addAttribute("product", product);
                    return "admin/product-form";
                }
                product.setCategory(cat);
            } else {
                product.setCategory(null);
            }

            if (featuredIndex == null) {
                featuredIndex = (product.getFeaturedIndex() != null ? product.getFeaturedIndex() : 1);
            }
            featuredIndex = Math.max(1, Math.min(5, featuredIndex));
            if (!hasImageAt(product, featuredIndex)) {
                Integer first = firstAvailableImageIndex(product);
                featuredIndex = (first != null ? first : 1);
            }
            product.setFeaturedIndex(featuredIndex);

            // LƯU và lấy đối tượng đã lưu
            Product saved = productService.save(product);

            try {
                com.bigkhoa.model.Laptop lap = laptopService.findById(saved.getId());
                if (lap != null) {
                    int safeFeatured = (featuredIndex != null && hasImageAt(saved, featuredIndex))
                            ? featuredIndex
                            : (firstAvailableImageIndex(saved) != null ? firstAvailableImageIndex(saved) : 1);
                    lap.setImageUrl("/product/" + saved.getId() + "/image/" + safeFeatured);

                    if (lap.getImages() != null) {
                        lap.getImages().clear();
                    } else {
                        lap.setImages(new java.util.ArrayList<>());
                    }
                    for (int idx = 1; idx <= 5; idx++) {
                        byte[] d = switch (idx) {
                            case 1 -> saved.getImage1();
                            case 2 -> saved.getImage2();
                            case 3 -> saved.getImage3();
                            case 4 -> saved.getImage4();
                            case 5 -> saved.getImage5();
                            default -> null;
                        };
                        if (d != null && d.length > 0) {
                            lap.getImages().add(
                                    new com.bigkhoa.model.LaptopImage(
                                            lap, "/product/" + saved.getId() + "/image/" + idx));
                        }
                    }
                    laptopService.save(lap);
                }
            } catch (Exception ignore) {
            }

            ra.addFlashAttribute("success", "Sản phẩm đã được lưu thành công");

            // Nếu có nút "Lưu & về danh sách"
            if ("save_list".equalsIgnoreCase(action)) {
                return "redirect:/admin/dashboard"; // nếu chưa có trang này, đổi thành /admin/dashboard
            }

            // MẶC ĐỊNH: quay về trang sửa để tự load lại
            return "redirect:/admin/product/" + saved.getId() + "/edit";

        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Lưu sản phẩm lỗi: " + ex.getMessage());
            return (product.getId() == null)
                    ? "redirect:/admin/product/new"
                    : "redirect:/admin/product/" + product.getId() + "/edit";
        }
    }

    private boolean hasImageAt(Product p, int idx) {
        byte[] data = switch (idx) {
            case 1 -> p.getImage1();
            case 2 -> p.getImage2();
            case 3 -> p.getImage3();
            case 4 -> p.getImage4();
            case 5 -> p.getImage5();
            default -> null;
        };
        return data != null && data.length > 0;
    }

    private Integer firstAvailableImageIndex(Product p) {
        for (int i = 1; i <= 5; i++) {
            if (hasImageAt(p, i))
                return i;
        }
        return null;
    }

    // ================== USERS ==================

    @GetMapping("/user/new")
    public String createUserForm(Model model) {
        model.addAttribute("user", new User());
        return "admin/user-form";
    }

    @PostMapping("/user/save")
    public String saveUser(@Valid @ModelAttribute("user") User user,
            BindingResult result,
            RedirectAttributes ra) {
        if (result.hasErrors()) {
            return "admin/user-form";
        }

        // Đảm bảo role được set (fallback nếu form không gửi)
        if (user.getRole() == null || user.getRole().trim().isEmpty()) {
            user.setRole("ROLE_USER");
        }

        try {
            userService.save(user);
            ra.addFlashAttribute("success", "Người dùng đã được lưu");
            return "redirect:/admin/dashboard";
        } catch (UsernameExistsException ex) {
            result.rejectValue("username", "username.exists", ex.getMessage());
            return "admin/user-form";
        } catch (EmailExistsException ex) {
            result.rejectValue("email", "email.exists", ex.getMessage());
            return "admin/user-form";
        } catch (Exception ex) {
            // Lỗi khác -> trả lại form và báo lỗi tổng quát
            result.reject("saveError", "Lưu người dùng thất bại: " + ex.getMessage());
            return "admin/user-form";
        }

    }

    @GetMapping("/user/{id}/edit")
    public String editUserForm(@PathVariable Long id, Model model, RedirectAttributes ra) {
        User u = userService.findById(id);
        if (u == null) {
            ra.addFlashAttribute("error", "Không tìm thấy người dùng");
            return "redirect:/admin/dashboard";
        }
        model.addAttribute("user", u);
        return "admin/user-form";
    }

    @PostMapping("/user/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes ra) {
        try {
            userService.deleteById(id);
            ra.addFlashAttribute("message", "Người dùng đã được xóa");
            ra.addFlashAttribute("success", "Người dùng đã được xóa");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Xóa người dùng thất bại: " + e.getMessage());
        }
        return "redirect:/admin/dashboard";

    }

    @GetMapping("/user/check-unique")
    public ResponseEntity<Map<String, Boolean>> checkUnique(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Long id // đang sửa thì truyền id để bỏ qua chính mình
    ) {
        boolean usernameTaken = false;
        boolean emailTaken = false;

        if (username != null && !username.isBlank()) {
            User u = userService.findByUsername(username.trim());
            if (u != null && (id == null || !u.getId().equals(id))) {
                usernameTaken = true;
            }
        }

        if (email != null && !email.isBlank()) {
            User u = userService.findByEmail(email.trim());
            if (u != null && (id == null || !u.getId().equals(id))) {
                emailTaken = true;
            }
        }

        return ResponseEntity.ok(Map.of(
                "usernameTaken", usernameTaken,
                "emailTaken", emailTaken));
    }

}
