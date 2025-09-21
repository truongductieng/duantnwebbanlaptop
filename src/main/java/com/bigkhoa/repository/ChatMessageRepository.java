package com.bigkhoa.repository;

import com.bigkhoa.model.ChatMessageEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {

    @Query("""
        SELECT m FROM ChatMessageEntity m
        WHERE (m.sender = :a AND m.recipient = :b)
           OR (m.sender = :b AND m.recipient = :a)
        ORDER BY m.sentAt ASC
    """)
    List<ChatMessageEntity> findByParticipants(@Param("a") String a, @Param("b") String b);

    // ========== Unread ==========

    // map: sender -> số tin nhắn họ gửi cho "me" mà "me" chưa đọc
    @Query("""
        SELECT m.sender AS sender, COUNT(m) AS cnt
        FROM ChatMessageEntity m
        WHERE m.recipient = :me AND m.readAt IS NULL
        GROUP BY m.sender
    """)
    List<UnreadCountRow> unreadCountBySender(@Param("me") String me);

    interface UnreadCountRow {
        String getSender();
        long getCnt();
    }

    @Modifying
    @Query("""
        UPDATE ChatMessageEntity m
        SET m.readAt = :now
        WHERE m.recipient = :me AND m.sender = :other AND m.readAt IS NULL
    """)
    int markReadForThread(@Param("me") String me,
                          @Param("other") String other,
                          @Param("now") LocalDateTime now);

    // ========== Threads mới nhất để đổ sidebar ==========

    @Query(value = """
        SELECT t.* FROM chat_messages t
        JOIN (
          SELECT 
            CASE WHEN sender = :me THEN recipient ELSE sender END AS other,
            MAX(sent_at) AS last_time
          FROM chat_messages
          WHERE sender = :me OR recipient = :me
          GROUP BY other
        ) j
        ON (
          (t.sender = :me AND t.recipient = j.other) 
          OR 
          (t.recipient = :me AND t.sender = j.other)
        )
        AND t.sent_at = j.last_time
        ORDER BY t.sent_at DESC
        """, nativeQuery = true)
    List<ChatMessageEntity> findLatestThreads(@Param("me") String me);
}
