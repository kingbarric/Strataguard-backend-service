package com.estatekit.infrastructure.repository;

import com.estatekit.core.entity.Unit;
import com.estatekit.core.enums.UnitStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UnitRepository extends JpaRepository<Unit, UUID> {

    @Query("SELECT u FROM Unit u WHERE u.estateId = :estateId AND u.tenantId = :tenantId AND u.deleted = false")
    Page<Unit> findAllByEstateIdAndTenantId(@Param("estateId") UUID estateId,
                                             @Param("tenantId") UUID tenantId,
                                             Pageable pageable);

    @Query("SELECT u FROM Unit u WHERE u.id = :id AND u.tenantId = :tenantId AND u.deleted = false")
    Optional<Unit> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM Unit u " +
            "WHERE u.unitNumber = :unitNumber AND u.estateId = :estateId AND u.tenantId = :tenantId AND u.deleted = false")
    boolean existsByUnitNumberAndEstateIdAndTenantId(@Param("unitNumber") String unitNumber,
                                                      @Param("estateId") UUID estateId,
                                                      @Param("tenantId") UUID tenantId);

    @Query("SELECT u FROM Unit u WHERE u.tenantId = :tenantId AND u.deleted = false AND u.status = :status")
    Page<Unit> findAllByTenantIdAndStatus(@Param("tenantId") UUID tenantId,
                                           @Param("status") UnitStatus status,
                                           Pageable pageable);

    @Query("SELECT COUNT(u) FROM Unit u WHERE u.estateId = :estateId AND u.tenantId = :tenantId AND u.deleted = false")
    long countByEstateIdAndTenantId(@Param("estateId") UUID estateId, @Param("tenantId") UUID tenantId);
}
