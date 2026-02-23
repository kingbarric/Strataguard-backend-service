package com.strataguard.infrastructure.repository;

import com.strataguard.core.entity.EstateCharge;
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
public interface EstateChargeRepository extends JpaRepository<EstateCharge, UUID> {

    @Query("SELECT e FROM EstateCharge e WHERE e.tenantId = :tenantId AND e.deleted = false")
    Page<EstateCharge> findAllByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT e FROM EstateCharge e WHERE e.id = :id AND e.tenantId = :tenantId AND e.deleted = false")
    Optional<EstateCharge> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT e FROM EstateCharge e WHERE e.estateId = :estateId AND e.tenantId = :tenantId AND e.deleted = false")
    Page<EstateCharge> findByEstateIdAndTenantId(@Param("estateId") UUID estateId, @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT e FROM EstateCharge e WHERE e.estateId = :estateId AND e.tenantId = :tenantId AND e.active = true AND e.deleted = false")
    List<EstateCharge> findActiveByEstateIdAndTenantId(@Param("estateId") UUID estateId, @Param("tenantId") UUID tenantId);

    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM EstateCharge e " +
            "WHERE e.name = :name AND e.estateId = :estateId AND e.tenantId = :tenantId AND e.deleted = false")
    boolean existsByNameAndEstateIdAndTenantId(@Param("name") String name, @Param("estateId") UUID estateId, @Param("tenantId") UUID tenantId);

    @Query("SELECT e FROM EstateCharge e WHERE e.tenantId = :tenantId AND e.deleted = false " +
            "AND (LOWER(e.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(e.category) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<EstateCharge> search(@Param("tenantId") UUID tenantId, @Param("search") String search, Pageable pageable);
}
