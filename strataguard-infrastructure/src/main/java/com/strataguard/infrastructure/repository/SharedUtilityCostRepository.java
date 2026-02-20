package com.strataguard.infrastructure.repository;

import com.strataguard.core.entity.SharedUtilityCost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SharedUtilityCostRepository extends JpaRepository<SharedUtilityCost, UUID> {

    @Query("SELECT s FROM SharedUtilityCost s WHERE s.id = :id AND s.tenantId = :tenantId AND s.deleted = false")
    Optional<SharedUtilityCost> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT s FROM SharedUtilityCost s WHERE s.estateId = :estateId AND s.tenantId = :tenantId AND s.deleted = false")
    Page<SharedUtilityCost> findByEstateIdAndTenantId(@Param("estateId") UUID estateId, @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT COUNT(s) FROM SharedUtilityCost s WHERE s.tenantId = :tenantId AND s.deleted = false")
    long countByTenantId(@Param("tenantId") UUID tenantId);
}
