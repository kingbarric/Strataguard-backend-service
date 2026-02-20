package com.strataguard.infrastructure.repository;

import com.strataguard.core.entity.Artisan;
import com.strataguard.core.enums.ArtisanCategory;
import com.strataguard.core.enums.ArtisanStatus;
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
public interface ArtisanRepository extends JpaRepository<Artisan, UUID> {

    @Query("SELECT a FROM Artisan a WHERE a.tenantId = :tenantId AND a.deleted = false")
    Page<Artisan> findAllByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT a FROM Artisan a WHERE a.id = :id AND a.tenantId = :tenantId AND a.deleted = false")
    Optional<Artisan> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT a FROM Artisan a WHERE a.estateId = :estateId AND a.tenantId = :tenantId AND a.deleted = false")
    Page<Artisan> findByEstateIdAndTenantId(@Param("estateId") UUID estateId, @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT a FROM Artisan a WHERE a.category = :category AND a.tenantId = :tenantId AND a.deleted = false")
    Page<Artisan> findByCategoryAndTenantId(@Param("category") ArtisanCategory category, @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT a FROM Artisan a WHERE a.estateId = :estateId AND a.category = :category AND a.tenantId = :tenantId AND a.deleted = false")
    Page<Artisan> findByEstateIdAndCategoryAndTenantId(@Param("estateId") UUID estateId, @Param("category") ArtisanCategory category, @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT a FROM Artisan a WHERE a.status = :status AND a.tenantId = :tenantId AND a.deleted = false")
    Page<Artisan> findByStatusAndTenantId(@Param("status") ArtisanStatus status, @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT a FROM Artisan a WHERE a.estateId = :estateId AND a.verified = true AND a.status = 'ACTIVE' AND a.tenantId = :tenantId AND a.deleted = false ORDER BY a.averageRating DESC")
    List<Artisan> findTopRatedByEstateId(@Param("estateId") UUID estateId, @Param("tenantId") UUID tenantId);

    @Query("SELECT a FROM Artisan a WHERE a.tenantId = :tenantId AND a.deleted = false AND " +
            "(LOWER(a.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(a.specialization) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Artisan> searchByNameOrSpecialization(@Param("query") String query, @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT COUNT(a) FROM Artisan a WHERE a.tenantId = :tenantId AND a.deleted = false")
    long countByTenantId(@Param("tenantId") UUID tenantId);
}
