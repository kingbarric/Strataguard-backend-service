package com.strataguard.infrastructure.repository;

import com.strataguard.core.entity.Announcement;
import com.strataguard.core.enums.AnnouncementAudience;
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
public interface AnnouncementRepository extends JpaRepository<Announcement, UUID> {

    @Query("SELECT a FROM Announcement a WHERE a.tenantId = :tenantId AND a.deleted = false ORDER BY a.pinned DESC, a.publishedAt DESC")
    Page<Announcement> findAllByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT a FROM Announcement a WHERE a.id = :id AND a.tenantId = :tenantId AND a.deleted = false")
    Optional<Announcement> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT a FROM Announcement a WHERE a.estateId = :estateId AND a.tenantId = :tenantId AND a.published = true AND a.deleted = false " +
            "AND (a.expiresAt IS NULL OR a.expiresAt > :now) ORDER BY a.pinned DESC, a.publishedAt DESC")
    Page<Announcement> findActiveByEstateId(@Param("estateId") UUID estateId, @Param("tenantId") UUID tenantId, @Param("now") Instant now, Pageable pageable);

    @Query("SELECT a FROM Announcement a WHERE a.estateId = :estateId AND a.tenantId = :tenantId AND a.deleted = false ORDER BY a.createdAt DESC")
    Page<Announcement> findByEstateIdAndTenantId(@Param("estateId") UUID estateId, @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT a FROM Announcement a WHERE a.estateId = :estateId AND a.audience = :audience AND a.tenantId = :tenantId AND a.published = true AND a.deleted = false " +
            "AND (a.expiresAt IS NULL OR a.expiresAt > :now) ORDER BY a.pinned DESC, a.publishedAt DESC")
    Page<Announcement> findByEstateIdAndAudience(@Param("estateId") UUID estateId, @Param("audience") AnnouncementAudience audience,
                                                  @Param("tenantId") UUID tenantId, @Param("now") Instant now, Pageable pageable);

    @Query("SELECT COUNT(a) FROM Announcement a WHERE a.tenantId = :tenantId AND a.deleted = false")
    long countByTenantId(@Param("tenantId") UUID tenantId);
}
