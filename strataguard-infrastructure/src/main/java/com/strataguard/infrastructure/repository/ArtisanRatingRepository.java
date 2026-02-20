package com.strataguard.infrastructure.repository;

import com.strataguard.core.entity.ArtisanRating;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ArtisanRatingRepository extends JpaRepository<ArtisanRating, UUID> {

    @Query("SELECT r FROM ArtisanRating r WHERE r.artisanId = :artisanId AND r.tenantId = :tenantId AND r.deleted = false ORDER BY r.createdAt DESC")
    Page<ArtisanRating> findByArtisanIdAndTenantId(@Param("artisanId") UUID artisanId, @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM ArtisanRating r " +
            "WHERE r.artisanId = :artisanId AND r.residentId = :residentId AND r.maintenanceRequestId = :requestId AND r.tenantId = :tenantId AND r.deleted = false")
    boolean existsByArtisanIdAndResidentIdAndMaintenanceRequestId(
            @Param("artisanId") UUID artisanId, @Param("residentId") UUID residentId,
            @Param("requestId") UUID requestId, @Param("tenantId") UUID tenantId);
}
