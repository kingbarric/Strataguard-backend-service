package com.strataguard.infrastructure.repository;

import com.strataguard.core.entity.ChatParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, UUID> {

    @Query("SELECT p FROM ChatParticipant p WHERE p.conversationId = :conversationId AND p.tenantId = :tenantId AND p.deleted = false")
    List<ChatParticipant> findByConversationIdAndTenantId(@Param("conversationId") UUID conversationId, @Param("tenantId") UUID tenantId);

    @Query("SELECT p FROM ChatParticipant p WHERE p.residentId = :residentId AND p.tenantId = :tenantId AND p.deleted = false")
    List<ChatParticipant> findByResidentIdAndTenantId(@Param("residentId") UUID residentId, @Param("tenantId") UUID tenantId);

    @Query("SELECT p FROM ChatParticipant p WHERE p.conversationId = :conversationId AND p.residentId = :residentId AND p.tenantId = :tenantId AND p.deleted = false")
    Optional<ChatParticipant> findByConversationIdAndResidentIdAndTenantId(
            @Param("conversationId") UUID conversationId,
            @Param("residentId") UUID residentId,
            @Param("tenantId") UUID tenantId);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM ChatParticipant p " +
            "WHERE p.conversationId = :conversationId AND p.residentId = :residentId AND p.tenantId = :tenantId AND p.deleted = false")
    boolean isParticipant(@Param("conversationId") UUID conversationId,
                          @Param("residentId") UUID residentId,
                          @Param("tenantId") UUID tenantId);

    @Modifying
    @Query("UPDATE ChatParticipant p SET p.lastReadAt = :readAt WHERE p.conversationId = :conversationId AND p.residentId = :residentId AND p.tenantId = :tenantId AND p.deleted = false")
    int updateLastReadAt(@Param("conversationId") UUID conversationId,
                         @Param("residentId") UUID residentId,
                         @Param("tenantId") UUID tenantId,
                         @Param("readAt") Instant readAt);

    @Query("SELECT p.conversationId FROM ChatParticipant p WHERE p.residentId = :residentId AND p.tenantId = :tenantId AND p.deleted = false")
    List<UUID> findConversationIdsByResidentIdAndTenantId(@Param("residentId") UUID residentId, @Param("tenantId") UUID tenantId);

    @Query("SELECT p FROM ChatParticipant p WHERE p.conversationId = :conversationId AND p.residentId != :excludeResidentId AND p.tenantId = :tenantId AND p.deleted = false")
    List<ChatParticipant> findOtherParticipants(@Param("conversationId") UUID conversationId,
                                                 @Param("excludeResidentId") UUID excludeResidentId,
                                                 @Param("tenantId") UUID tenantId);
}
