package com.strataguard.infrastructure.repository;

import com.strataguard.core.entity.EstateChargeExclusion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EstateChargeExclusionRepository extends JpaRepository<EstateChargeExclusion, UUID> {

    @Query("SELECT e FROM EstateChargeExclusion e WHERE e.estateChargeId = :chargeId AND e.tenantId = :tenantId AND e.deleted = false")
    List<EstateChargeExclusion> findByEstateChargeIdAndTenantId(@Param("chargeId") UUID chargeId, @Param("tenantId") UUID tenantId);

    @Query("SELECT e.tenancyId FROM EstateChargeExclusion e WHERE e.estateChargeId = :chargeId AND e.tenantId = :tenantId AND e.deleted = false")
    List<UUID> findExcludedTenancyIds(@Param("chargeId") UUID chargeId, @Param("tenantId") UUID tenantId);

    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM EstateChargeExclusion e " +
            "WHERE e.estateChargeId = :chargeId AND e.tenancyId = :tenancyId AND e.tenantId = :tenantId AND e.deleted = false")
    boolean existsByEstateChargeIdAndTenancyIdAndTenantId(@Param("chargeId") UUID chargeId, @Param("tenancyId") UUID tenancyId, @Param("tenantId") UUID tenantId);

    @Query("SELECT e FROM EstateChargeExclusion e WHERE e.id = :id AND e.tenantId = :tenantId AND e.deleted = false")
    Optional<EstateChargeExclusion> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);
}
