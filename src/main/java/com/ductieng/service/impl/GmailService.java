package com.ductieng.service.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import com.ductieng.model.Order;
import com.ductieng.repository.OrderRepository;
import com.ductieng.util.Fmt;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class GmailService {

    private static final Logger log = LoggerFactory.getLogger(GmailService.class);

    private static final Locale VI = new Locale("vi", "VN");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final Fmt fmtBean; // bean format
    private final OrderRepository orderRepository;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.mail.from-name:Laptop Shop}")
    private String fromName;

    @Value("${app.public-base-url:http://localhost:8080}")
    private String publicBaseUrl;

    public GmailService(JavaMailSender mailSender,
                        SpringTemplateEngine templateEngine,
                        Fmt fmtBean,
                        OrderRepository orderRepository) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.fmtBean = fmtBean;
        this.orderRepository = orderRepository;
    }

    public void sendOrderConfirmationEmail(Order order) {
        if (order == null) {
            log.warn("[Mail] Bỏ qua gửi vì order=null");
            return;
        }

        // Luôn nạp lại đơn cùng items & product để tránh LazyInitializationException
        try {
            Order loaded = orderRepository.findByIdWithItems(order.getId()).orElse(order);
            order = loaded;
        } catch (Exception e) {
            log.warn("[Mail] Không thể nạp lại order với items: {} (vẫn tiếp tục với đối tượng hiện có)", e.getMessage());
        }

        String to = resolveRecipientEmail(order);
        if (to == null) {
            log.warn("[Mail] Order #{} KHÔNG có email hợp lệ -> BỎ QUA gửi mail", order.getId());
            return;
        }

        try {
            String html = renderOrderConfirmationHtml(order);

            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, false, StandardCharsets.UTF_8.name());

            helper.setTo(to);
            helper.setFrom(new InternetAddress(fromEmail, fromName, StandardCharsets.UTF_8.name()));
            helper.setSubject("[Laptop Shop] Xác nhận đơn hàng #" + order.getId());
            helper.setText(html, true);

            log.info("[Mail] Đang gửi xác nhận đơn #{} tới {}", order.getId(), to);
            mailSender.send(mime);
            log.info("[Mail] Đã gửi email xác nhận cho đơn #{} -> {}", order.getId(), to);
        } catch (MessagingException me) {
            log.error("[Mail] Lỗi gửi mail cho đơn #{}: {}", order.getId(), me.getMessage(), me);
        } catch (Exception ex) {
            log.error("[Mail] Lỗi không mong muốn khi gửi mail cho đơn #{}: {}", order.getId(), ex.getMessage(), ex);
        }
    }

    private String renderOrderConfirmationHtml(Order order) {
        Context ctx = new Context(VI);
        ctx.setVariable("order", order);
        ctx.setVariable("items", order.getItems() != null ? order.getItems() : java.util.List.of());

        if (order.getCreatedAt() != null) {
            try {
                ctx.setVariable("orderDate", DATE_FMT.format(order.getCreatedAt()));
            } catch (Exception ignore) { /* không chặn render */ }
        }

        // Đường dẫn xem đơn
        try {
            String link = publicBaseUrl.replaceAll("/+$","") + "/confirmation/" + order.getId();
            ctx.setVariable("orderLink", link);
        } catch (Exception ignore) { }

        // Bơm helper 'fmt' vào context (fallback nếu bean null)
        Fmt fmtForCtx = (fmtBean != null ? fmtBean : new Fmt());
        ctx.setVariable("fmt", fmtForCtx);

        if (log.isDebugEnabled()) {
            log.debug("[Mail] fmt trong context: {}", (fmtBean != null ? "bean" : "fallback-new"));
        }

        return templateEngine.process("email/order-confirmation", ctx);
    }

    private String resolveRecipientEmail(Order order) {
        String to = null;
        try {
            if (hasText(order.getRecipientEmail())) {
                to = order.getRecipientEmail().trim();
            } else if (order.getCustomer() != null && hasText(order.getCustomer().getEmail())) {
                to = order.getCustomer().getEmail().trim();
            } else if (order.getCustomer() != null &&
                      order.getCustomer().getUsername() != null &&
                      order.getCustomer().getUsername().contains("@")) {
                to = order.getCustomer().getUsername().trim();
            }
            if (to == null || to.isBlank() || !to.contains("@")) return null;
            return to.replace("\u00A0", " ").trim(); // loại bỏ NBSP nếu có
        } catch (Exception ex) {
            log.warn("[Mail] Không thể xác định email người nhận cho đơn #{}: {}", order.getId(), ex.getMessage());
            return null;
        }
    }

    private boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
