package com.bigkhoa.controller;

import com.bigkhoa.model.ChatMessage;
import com.bigkhoa.model.ChatMessageEntity;
import com.bigkhoa.repository.ChatMessageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class ChatController {

    private final SimpMessagingTemplate template;
    private final ChatMessageRepository repo;
    private final SimpUserRegistry userRegistry;

    /**
     * Có thể khai báo nhiều biến thể tên admin (CSV). Phần tử ĐẦU TIÊN là tên "chuẩn hoá" để lưu DB.
     * Ví dụ: app.admin.username=admin,khoa,admin@gmail.com
     */
    @Value("${app.admin.username:admin}")
    private String adminConfigCsv;

    public ChatController(SimpMessagingTemplate template,
                          ChatMessageRepository repo,
                          SimpUserRegistry userRegistry) {
        this.template = template;
        this.repo = repo;
        this.userRegistry = userRegistry;
    }

    private boolean isAdmin(Principal principal) {
        if (principal instanceof Authentication auth) {
            for (GrantedAuthority ga : auth.getAuthorities()) {
                if ("ROLE_ADMIN".equals(ga.getAuthority())) return true;
            }
        }
        return false;
    }

    private List<String> adminCandidates() {
        if (adminConfigCsv == null) return List.of("admin");
        return Arrays.stream(adminConfigCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /** Tên admin chuẩn hoá (ổn định để lưu DB) */
    private String adminCanonical() {
        List<String> c = adminCandidates();
        return c.isEmpty() ? "admin" : c.get(0);
    }

    /** Tìm principal (username đăng nhập thực tế) của admin đang online để deliver realtime */
    private Optional<String> adminOnlinePrincipal() {
        Set<String> online = userRegistry.getUsers().stream()
                .map(SimpUser::getName)
                .collect(Collectors.toSet());
        for (String c : adminCandidates()) {
            for (String on : online) {
                if (on.equalsIgnoreCase(c)) {
                    return Optional.of(on); // ví dụ "khoa"
                }
            }
        }
        return Optional.empty();
    }

    // Client gửi vào /app/chat.send
    @MessageMapping("/chat.send")
    public void sendMessage(ChatMessage payload, Principal principal) {
        if (payload == null) return;

        String me = (principal != null && principal.getName() != null) ? principal.getName() : "anonymous";
        boolean senderIsAdmin = isAdmin(principal);

        String content = payload.getContent() == null ? "" : payload.getContent().trim();
        if (content.isEmpty()) return;

        // Xác định nơi lưu DB (dbTo) và nơi deliver realtime (deliverTo)
        String dbTo;
        String deliverTo;

        if (senderIsAdmin) {
            String target = payload.getTo() == null ? "" : payload.getTo().trim();
            if (target.isBlank() || target.equalsIgnoreCase(me)) return;
            dbTo = target;       // admin -> user cụ thể
            deliverTo = target;  // realtime tới user đó
        } else {
            dbTo = adminCanonical();                               // DB luôn lưu admin chuẩn
            deliverTo = adminOnlinePrincipal().orElse(dbTo);       // realtime tới admin thực đang online (nếu có)
        }

        // 👇 LƯU DB: nếu người gửi là admin thì chuẩn hoá sender = adminCanonical()
        String dbFrom = senderIsAdmin ? adminCanonical() : me;

        ChatMessageEntity e = new ChatMessageEntity(
                dbFrom,             // lưu "admin" thay vì "khoa"
                dbTo,
                content,
                LocalDateTime.now(),
                senderIsAdmin ? "ADMIN" : "USER"
        );
        repo.save(e);

        // Gửi realtime: from = tên thật đang đăng nhập (để FE so sánh với myUsername), to = deliverTo
        ChatMessage out = new ChatMessage(me, deliverTo, content, e.getSentAt().toString());
        template.convertAndSendToUser(deliverTo, "/queue/messages", out); // người nhận
        template.convertAndSendToUser(me, "/queue/messages", out);        // echo người gửi
    }

    // View user/admin (nếu đang dùng 1 view chung): /chat
    @GetMapping("/chat")
    public String chatPage(@RequestParam(value = "with", required = false) String with,
                           Principal principal,
                           Model model) {
        String me = principal != null ? principal.getName() : null;
        boolean admin = isAdmin(principal);

        // User: luôn chat với "admin chuẩn"; Admin: có thể ?with=username
        model.addAttribute("username", me);
        model.addAttribute("isAdmin", admin);
        model.addAttribute("adminUsername", adminCanonical());     // FE biết alias chuẩn của admin
        model.addAttribute("with", admin ? with : adminCanonical());
        return "chat";
    }
}
