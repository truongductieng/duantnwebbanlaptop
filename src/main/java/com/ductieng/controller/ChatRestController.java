package com.ductieng.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import com.ductieng.model.ChatMessage;
import com.ductieng.model.ChatMessageEntity;
import com.ductieng.repository.ChatMessageRepository;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
public class ChatRestController {

    private final ChatMessageRepository repo;
    private final SimpMessagingTemplate template;

    /** CSV: phần tử đầu là tên chuẩn hoá (để lưu DB), các phần sau là alias (vd username thật "khoa") */
    @Value("${app.admin.username:admin}")
    private String adminConfigCsv;

    public ChatRestController(ChatMessageRepository repo, SimpMessagingTemplate template) {
        this.repo = repo;
        this.template = template;
    }

    // ===== Helpers =====
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

    private String adminCanonical() {
        List<String> c = adminCandidates();
        return c.isEmpty() ? "admin" : c.get(0);
    }

    /** Nếu là admin -> trả về tên chuẩn hoá; ngược lại trả về username thật */
    private String canonicalMe(Principal principal) {
        String me = principal != null ? principal.getName() : null;
        return isAdmin(principal) ? adminCanonical() : me;
    }

    private String otherOf(ChatMessageEntity e, String me) {
        return me != null && me.equals(e.getSender()) ? e.getRecipient() : e.getSender();
    }

    // ===== DTOs =====
    private record NewMessageDto(String to, String content) {}
    public  record PartnerDto(String username, String lastMessage, String lastTime) {}
    private record Ok(int updated) {}

    // ===== APIs =====

    // Lịch sử 2 chiều. User thường: luôn ép 'with' = admin chuẩn hoá
    @GetMapping("/history")
    @Transactional
    public List<ChatMessage> history(@RequestParam(value = "with", required = false) String with,
                                     Principal principal) {
        String meCanonical = canonicalMe(principal);   // admin -> "admin", user -> username
        boolean admin = isAdmin(principal);

        String other = admin ? (with == null ? "" : with.trim()) : adminCanonical();
        if (other.isBlank()) return List.of();

        // mark-read tất cả tin gửi tới "me" từ "other"
        repo.markReadForThread(meCanonical, other, LocalDateTime.now());

        // Lấy lịch sử chính theo tên chuẩn
        List<ChatMessageEntity> all = new ArrayList<>(repo.findByParticipants(meCanonical, other));

        // Gộp thêm lịch sử cũ nếu từng lưu bằng tên thật của admin (ví dụ "khoa")
        if (admin) {
            String meReal = principal != null ? principal.getName() : null;
            if (meReal != null && !meReal.equalsIgnoreCase(meCanonical)) {
                all.addAll(repo.findByParticipants(meReal, other));
            }
        }

        // Sort + dedup
        all.sort(Comparator.comparing(ChatMessageEntity::getSentAt));
        LinkedHashMap<String, ChatMessageEntity> uniq = new LinkedHashMap<>();
        for (ChatMessageEntity e : all) {
            String key = (e.getId() != null)
                    ? "id:" + e.getId()
                    : (e.getSender() + "|" + e.getRecipient() + "|" + e.getContent() + "|" +
                       (e.getSentAt() != null ? e.getSentAt().toString() : "null"));
            uniq.putIfAbsent(key, e);
        }
        List<ChatMessageEntity> merged = new ArrayList<>(uniq.values());

        // Map tên cho FE của admin: "admin" -> tên thật đang đăng nhập (vd "khoa")
        String myDisplay = principal != null ? principal.getName() : meCanonical;

        return merged.stream().map(e -> {
            String from = e.getSender();
            String to   = e.getRecipient();
            if (admin) {
                if (from.equalsIgnoreCase(adminCanonical())) from = myDisplay;
                if (to.equalsIgnoreCase(adminCanonical()))   to   = myDisplay;
            }
            return new ChatMessage(from, to, e.getContent(),
                    e.getSentAt() != null ? e.getSentAt().toString() : null);
        }).toList();
    }

    // Gửi tin nhắn (fallback khi không dùng WS)
    @PostMapping("/send")
    public ChatMessage send(@RequestBody NewMessageDto dto, Principal principal) {
        String meReal = principal != null ? principal.getName() : "anonymous";
        boolean admin = isAdmin(principal);

        String to = admin ? (dto.to() == null ? "" : dto.to().trim()) : adminCanonical();
        String content = dto.content() == null ? "" : dto.content().trim();
        if (content.isEmpty()) throw new IllegalArgumentException("Thiếu nội dung");
        if (admin && (to.isBlank() || to.equalsIgnoreCase(meReal))) {
            throw new IllegalArgumentException("Admin phải chọn người nhận hợp lệ");
        }

        // LƯU DB: admin gửi -> chuẩn hoá sender = adminCanonical()
        String dbFrom = admin ? adminCanonical() : meReal;

        ChatMessageEntity saved = repo.save(new ChatMessageEntity(
                dbFrom, to, content, LocalDateTime.now(), admin ? "ADMIN" : "USER"
        ));

        // Realtime: from dùng tên thật đang đăng nhập để FE so sánh === myUsername
        ChatMessage out = new ChatMessage(meReal, to, content, saved.getSentAt().toString());
        template.convertAndSendToUser(to, "/queue/messages", out);
        template.convertAndSendToUser(meReal, "/queue/messages", out);
        return out;
    }

    // Danh sách thread gần nhất — chỉ Admin cần
    @GetMapping("/partners")
    @PreAuthorize("hasRole('ADMIN')")
    public List<PartnerDto> partners(Principal principal) {
        String meCanonical = canonicalMe(principal);  // "admin"
        String meReal = principal != null ? principal.getName() : meCanonical;

        Map<String, ChatMessageEntity> latest = new HashMap<>();

        // từ nguồn chuẩn hoá
        for (ChatMessageEntity e : repo.findLatestThreads(meCanonical)) {
            String other = otherOf(e, meCanonical);
            latest.put(other, e);
        }
        // gộp thêm nguồn cũ (nếu có)
        if (!meReal.equalsIgnoreCase(meCanonical)) {
            for (ChatMessageEntity e : repo.findLatestThreads(meReal)) {
                String other = otherOf(e, meReal);
                ChatMessageEntity cur = latest.get(other);
                if (cur == null || (e.getSentAt() != null &&
                        (cur.getSentAt() == null || e.getSentAt().isAfter(cur.getSentAt())))) {
                    latest.put(other, e);
                }
            }
        }

        return latest.entrySet().stream()
                .sorted((a, b) -> {
                    LocalDateTime ta = Optional.ofNullable(a.getValue().getSentAt()).orElse(LocalDateTime.MIN);
                    LocalDateTime tb = Optional.ofNullable(b.getValue().getSentAt()).orElse(LocalDateTime.MIN);
                    return tb.compareTo(ta); // desc
                })
                .map(en -> new PartnerDto(
                        en.getKey(),
                        en.getValue().getContent(),
                        en.getValue().getSentAt() != null ? en.getValue().getSentAt().toString() : null
                ))
                .toList();
    }

    // Map chưa đọc (dùng cho badge)
    @GetMapping("/unread-map")
    public Map<String, Long> unreadMap(Principal principal) {
        String me = canonicalMe(principal); // "admin" cho admin; user = username thật
        Map<String, Long> map = new HashMap<>();
        repo.unreadCountBySender(me).forEach(row -> map.put(row.getSender(), row.getCnt()));
        return map;
    }

    // Đánh dấu đã đọc 1 thread
    @PostMapping("/mark-read")
    @Transactional
    public Ok markRead(@RequestParam("with") String other, Principal principal) {
        String me = canonicalMe(principal); // "admin" cho admin
        boolean admin = isAdmin(principal);
        if (!admin) other = adminCanonical(); // user chỉ được mark-read với admin
        int updated = repo.markReadForThread(me, other, LocalDateTime.now());
        return new Ok(updated);
    }
}
