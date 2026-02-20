package com.strataguard.infrastructure.repository;

import com.strataguard.core.entity.UtilityMeter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UtilityMeterRepository extends JpaRepository<UtilityMeter, UUID> {

    @Query("SELECT m FROM UtilityMeter m WHERE m.id = :id AND m.tenantId = :tenantId AND m.deleted = false")
    Optional<UtilityMeter> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT m FROM UtilityMeter m WHERE m.unitId = :unitId AND m.tenantId = :tenantId AND m.deleted = false")
    Page<UtilityMeter> findByUnitIdAndTenantId(@Param("unitId") UUID unitId, @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT m FROM UtilityMeter m WHERE m.estateId = :estateId AND m.tenantId = :tenantId AND m.deleted = false")
    Page<UtilityMeter> findByEstateIdAndTenantId(@Param("estateId") UUID estateId, @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM UtilityMeter m " +
            "WHERE m.meterNumber = :meterNumber AND m.tenantId = :tenantId AND m.deleted = false")
    boolean existsByMeterNumberAndTenantId(@Param("meterNumber") String meterNumber, @Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(m) FROM UtilityMeter m WHERE m.tenantId = :tenantId AND m.deleted = false")
    long countByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(m) FROM UtilityMeter m WHERE m.tenantId = :tenantId AND m.active = true AND m.deleted = false")
    long countActiveByTenantId(@Param("tenantId") UUID tenantId);
}
