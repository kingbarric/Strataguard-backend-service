package com.strataguard.infrastructure.repository;

import com.strataguard.core.entity.ChatConversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatConversationRepository extends JpaRepository<ChatConversation, UUID> {

    @Query("SELECT c FROM ChatConversation c WHERE c.id = :id AND c.tenantId = :tenantId AND c.deleted = false")
    Optional<ChatConversation> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT c FROM ChatConversation c WHERE c.id IN :ids AND c.tenantId = :tenantId AND c.deleted = false ORDER BY c.lastMessageAt DESC NULLS LAST")
    Page<ChatConversation> findByIdInAndTenantId(@Param("ids") List<UUID> ids, @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT c FROM ChatConversation c WHERE c.estateId = :estateId AND c.tenantId = :tenantId AND c.deleted = false ORDER BY c.lastMessageAt DESC NULLS LAST")
    Page<ChatConversation> findByEstateIdAndTenantId(@Param("estateId") UUID estateId, @Param("tenantId") UUID tenantId, Pageable pageable);
}
