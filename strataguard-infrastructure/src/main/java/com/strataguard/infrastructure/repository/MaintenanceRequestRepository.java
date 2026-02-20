package com.strataguard.infrastructure.repository;

import com.strataguard.core.entity.MaintenanceRequest;
import com.strataguard.core.enums.MaintenanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MaintenanceRequestRepository extends JpaRepository<MaintenanceRequest, UUID> {

    @Query("SELECT m FROM MaintenanceRequest m WHERE m.id = :id AND m.tenantId = :tenantId AND m.deleted = false")
    Optional<MaintenanceRequest> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT m FROM MaintenanceRequest m WHERE m.residentId = :residentId AND m.tenantId = :tenantId AND m.deleted = false")
    Page<MaintenanceRequest> findByResidentIdAndTenantId(@Param("residentId") UUID residentId, @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT m FROM MaintenanceRequest m WHERE m.unitId = :unitId AND m.tenantId = :tenantId AND m.deleted = false")
    Page<MaintenanceRequest> findByUnitIdAndTenantId(@Param("unitId") UUID unitId, @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT m FROM MaintenanceRequest m WHERE m.status = :status AND m.tenantId = :tenantId AND m.deleted = false")
    Page<MaintenanceRequest> findByStatusAndTenantId(@Param("status") MaintenanceStatus status, @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT m FROM MaintenanceRequest m WHERE m.estateId = :estateId AND m.tenantId = :tenantId AND m.deleted = false")
    Page<MaintenanceRequest> findByEstateIdAndTenantId(@Param("estateId") UUID estateId, @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT m FROM MaintenanceRequest m WHERE m.tenantId = :tenantId AND m.deleted = false")
    Page<MaintenanceRequest> findAllByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT m FROM MaintenanceRequest m WHERE m.slaBreached = false AND m.slaDeadline < :now " +
            "AND m.status NOT IN ('RESOLVED', 'CLOSED', 'CANCELLED') AND m.tenantId = :tenantId AND m.deleted = false")
    List<MaintenanceRequest> findSlaBreachedRequests(@Param("now") Instant now, @Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(m) FROM MaintenanceRequest m WHERE m.status = :status AND m.tenantId = :tenantId AND m.deleted = false")
    long countByStatusAndTenantId(@Param("status") MaintenanceStatus status, @Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(m) FROM MaintenanceRequest m WHERE m.tenantId = :tenantId AND m.deleted = false")
    long countByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(m) FROM MaintenanceRequest m WHERE m.slaBreached = true AND m.tenantId = :tenantId AND m.deleted = false")
    long countSlaBreachedByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT AVG(m.satisfactionRating) FROM MaintenanceRequest m WHERE m.satisfactionRating IS NOT NULL AND m.tenantId = :tenantId AND m.deleted = false")
    Double averageSatisfactionRatingByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT m FROM MaintenanceRequest m WHERE m.tenantId = :tenantId AND m.deleted = false " +
            "AND (LOWER(m.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(m.description) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(m.requestNumber) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<MaintenanceRequest> search(@Param("tenantId") UUID tenantId, @Param("query") String query, Pageable pageable);

    @Query("SELECT COUNT(m) FROM MaintenanceRequest m WHERE m.tenantId = :tenantId AND m.deleted = false " +
            "AND m.requestNumber LIKE :prefix")
    long countByRequestNumberPrefix(@Param("tenantId") UUID tenantId, @Param("prefix") String prefix);
}
