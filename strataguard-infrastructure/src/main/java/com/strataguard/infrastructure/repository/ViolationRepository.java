package com.strataguard.infrastructure.repository;

import com.strataguard.core.entity.Violation;
import com.strataguard.core.enums.ViolationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ViolationRepository extends JpaRepository<Violation, UUID> {

    @Query("SELECT v FROM Violation v WHERE v.tenantId = :tenantId AND v.deleted = false ORDER BY v.createdAt DESC")
    Page<Violation> findAllByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT v FROM Violation v WHERE v.id = :id AND v.tenantId = :tenantId AND v.deleted = false")
    Optional<Violation> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT v FROM Violation v WHERE v.estateId = :estateId AND v.tenantId = :tenantId AND v.deleted = false ORDER BY v.createdAt DESC")
    Page<Violation> findByEstateIdAndTenantId(@Param("estateId") UUID estateId, @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT v FROM Violation v WHERE v.unitId = :unitId AND v.tenantId = :tenantId AND v.deleted = false ORDER BY v.createdAt DESC")
    Page<Violation> findByUnitIdAndTenantId(@Param("unitId") UUID unitId, @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT v FROM Violation v WHERE v.status = :status AND v.tenantId = :tenantId AND v.deleted = false ORDER BY v.createdAt DESC")
    Page<Violation> findByStatusAndTenantId(@Param("status") ViolationStatus status, @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT COUNT(v) FROM Violation v WHERE v.tenantId = :tenantId AND v.deleted = false")
    long countByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(v) FROM Violation v WHERE v.status = :status AND v.tenantId = :tenantId AND v.deleted = false")
    long countByStatusAndTenantId(@Param("status") ViolationStatus status, @Param("tenantId") UUID tenantId);
}
