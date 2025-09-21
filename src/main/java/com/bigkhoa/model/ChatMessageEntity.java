package com.bigkhoa.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
public class ChatMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, length=100)
    private String sender;

    @Column(nullable=false, length=100)
    private String recipient;

    @Column(nullable=false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "sent_at", nullable=false)
    private LocalDateTime sentAt;

    // Vai trò của người gửi (USER/ADMIN) — giữ nguyên theo dự án anh
    @Column(name = "sender_role", length=20)
    private String senderRole;

    // NULL = chưa đọc; khác NULL = đã đọc lúc <readAt>
    @Column(name = "read_at")
    private LocalDateTime readAt;

    public ChatMessageEntity() {}

    public ChatMessageEntity(String sender, String recipient, String content,
                             LocalDateTime sentAt, String senderRole) {
        this.sender = sender;
        this.recipient = recipient;
        this.content = content;
        this.sentAt = sentAt;
        this.senderRole = senderRole;
    }

    // getters & setters
    public Long getId() { return id; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }

    public String getSenderRole() { return senderRole; }
    public void setSenderRole(String senderRole) { this.senderRole = senderRole; }

    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }
}
