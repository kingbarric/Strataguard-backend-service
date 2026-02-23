package com.strataguard.infrastructure.repository;

import com.strataguard.core.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    @Query("SELECT m FROM ChatMessage m WHERE m.conversationId = :conversationId AND m.tenantId = :tenantId AND m.deleted = false ORDER BY m.createdAt DESC")
    Page<ChatMessage> findByConversationIdAndTenantId(@Param("conversationId") UUID conversationId, @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT m FROM ChatMessage m WHERE m.id = :id AND m.tenantId = :tenantId AND m.deleted = false")
    Optional<ChatMessage> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.conversationId = :conversationId AND m.tenantId = :tenantId AND m.deleted = false AND m.createdAt > :since")
    long countUnreadMessages(@Param("conversationId") UUID conversationId, @Param("tenantId") UUID tenantId, @Param("since") Instant since);

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.conversationId = :conversationId AND m.tenantId = :tenantId AND m.deleted = false")
    long countAllMessages(@Param("conversationId") UUID conversationId, @Param("tenantId") UUID tenantId);
}
