package com.strataguard.infrastructure.repository;

import com.strataguard.core.entity.TenantCharge;
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
public interface TenantChargeRepository extends JpaRepository<TenantCharge, UUID> {

    @Query("SELECT t FROM TenantCharge t WHERE t.tenantId = :tenantId AND t.deleted = false")
    Page<TenantCharge> findAllByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT t FROM TenantCharge t WHERE t.id = :id AND t.tenantId = :tenantId AND t.deleted = false")
    Optional<TenantCharge> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT t FROM TenantCharge t WHERE t.tenancyId = :tenancyId AND t.tenantId = :tenantId AND t.deleted = false")
    Page<TenantCharge> findByTenancyIdAndTenantId(@Param("tenancyId") UUID tenancyId, @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT t FROM TenantCharge t WHERE t.tenancyId = :tenancyId AND t.tenantId = :tenantId AND t.active = true AND t.deleted = false")
    List<TenantCharge> findActiveByTenancyIdAndTenantId(@Param("tenancyId") UUID tenancyId, @Param("tenantId") UUID tenantId);

    @Query("SELECT t FROM TenantCharge t WHERE t.estateId = :estateId AND t.tenantId = :tenantId AND t.deleted = false")
    Page<TenantCharge> findByEstateIdAndTenantId(@Param("estateId") UUID estateId, @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM TenantCharge t " +
            "WHERE t.name = :name AND t.tenancyId = :tenancyId AND t.tenantId = :tenantId AND t.deleted = false")
    boolean existsByNameAndTenancyIdAndTenantId(@Param("name") String name, @Param("tenancyId") UUID tenancyId, @Param("tenantId") UUID tenantId);
}
