package com.strataguard.infrastructure.repository;

import com.strataguard.core.entity.Tenancy;
import com.strataguard.core.enums.TenancyStatus;
import com.strataguard.core.enums.TenancyType;
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
public interface TenancyRepository extends JpaRepository<Tenancy, UUID> {

    @Query("SELECT t FROM Tenancy t WHERE t.tenantId = :tenantId AND t.deleted = false")
    Page<Tenancy> findAllByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT t FROM Tenancy t WHERE t.id = :id AND t.tenantId = :tenantId AND t.deleted = false")
    Optional<Tenancy> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT t FROM Tenancy t WHERE t.residentId = :residentId AND t.tenantId = :tenantId AND t.deleted = false")
    Page<Tenancy> findByResidentIdAndTenantId(@Param("residentId") UUID residentId,
                                               @Param("tenantId") UUID tenantId,
                                               Pageable pageable);

    @Query("SELECT t FROM Tenancy t WHERE t.unitId = :unitId AND t.tenantId = :tenantId AND t.deleted = false")
    Page<Tenancy> findByUnitIdAndTenantId(@Param("unitId") UUID unitId,
                                           @Param("tenantId") UUID tenantId,
                                           Pageable pageable);

    @Query("SELECT t FROM Tenancy t JOIN Unit u ON t.unitId = u.id " +
            "WHERE u.estateId = :estateId AND t.tenantId = :tenantId AND t.deleted = false")
    Page<Tenancy> findByEstateIdAndTenantId(@Param("estateId") UUID estateId,
                                             @Param("tenantId") UUID tenantId,
                                             Pageable pageable);

    @Query("SELECT t FROM Tenancy t WHERE t.unitId = :unitId AND t.tenantId = :tenantId " +
            "AND t.status = 'ACTIVE' AND t.deleted = false")
    List<Tenancy> findActiveByUnitIdAndTenantId(@Param("unitId") UUID unitId,
                                                 @Param("tenantId") UUID tenantId);

    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM Tenancy t " +
            "WHERE t.unitId = :unitId AND t.tenancyType = :tenancyType AND t.status = 'ACTIVE' " +
            "AND t.tenantId = :tenantId AND t.deleted = false")
    boolean existsActiveByUnitIdAndTenancyTypeAndTenantId(@Param("unitId") UUID unitId,
                                                           @Param("tenancyType") TenancyType tenancyType,
                                                           @Param("tenantId") UUID tenantId);
}
