package com.strataguard.infrastructure.repository;

import com.strataguard.core.entity.EmergencyAlert;
import com.strataguard.core.enums.EmergencyAlertStatus;
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
public interface EmergencyAlertRepository extends JpaRepository<EmergencyAlert, UUID> {

    @Query("SELECT a FROM EmergencyAlert a WHERE a.id = :id AND a.tenantId = :tenantId AND a.deleted = false")
    Optional<EmergencyAlert> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT a FROM EmergencyAlert a WHERE a.estateId = :estateId AND a.tenantId = :tenantId AND a.deleted = false")
    Page<EmergencyAlert> findByEstateIdAndTenantId(@Param("estateId") UUID estateId, @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT a FROM EmergencyAlert a WHERE a.residentId = :residentId AND a.tenantId = :tenantId AND a.deleted = false")
    Page<EmergencyAlert> findByResidentIdAndTenantId(@Param("residentId") UUID residentId, @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT a FROM EmergencyAlert a WHERE a.estateId = :estateId AND a.tenantId = :tenantId AND a.deleted = false " +
            "AND a.status NOT IN (com.strataguard.core.enums.EmergencyAlertStatus.RESOLVED, com.strataguard.core.enums.EmergencyAlertStatus.FALSE_ALARM)")
    List<EmergencyAlert> findActiveByEstateIdAndTenantId(@Param("estateId") UUID estateId, @Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(a) FROM EmergencyAlert a WHERE a.status = :status AND a.tenantId = :tenantId AND a.deleted = false")
    long countByStatusAndTenantId(@Param("status") EmergencyAlertStatus status, @Param("tenantId") UUID tenantId);

    @Query("SELECT AVG(a.responseTimeSeconds) FROM EmergencyAlert a WHERE a.responseTimeSeconds IS NOT NULL AND a.tenantId = :tenantId AND a.deleted = false")
    Double avgResponseTimeByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(a) FROM EmergencyAlert a WHERE a.tenantId = :tenantId AND a.deleted = false " +
            "AND a.status NOT IN (com.strataguard.core.enums.EmergencyAlertStatus.RESOLVED, com.strataguard.core.enums.EmergencyAlertStatus.FALSE_ALARM)")
    long countActiveByTenantId(@Param("tenantId") UUID tenantId);
}
