package com.strataguard.infrastructure.repository;

import com.strataguard.core.entity.PatrolSession;
import com.strataguard.core.enums.PatrolSessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatrolSessionRepository extends JpaRepository<PatrolSession, UUID> {

    @Query("SELECT p FROM PatrolSession p WHERE p.id = :id AND p.tenantId = :tenantId AND p.deleted = false")
    Optional<PatrolSession> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT p FROM PatrolSession p WHERE p.staffId = :staffId AND p.tenantId = :tenantId AND p.deleted = false")
    Page<PatrolSession> findByStaffIdAndTenantId(@Param("staffId") UUID staffId, @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT p FROM PatrolSession p WHERE p.estateId = :estateId AND p.tenantId = :tenantId AND p.deleted = false")
    Page<PatrolSession> findByEstateIdAndTenantId(@Param("estateId") UUID estateId, @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT p FROM PatrolSession p WHERE p.staffId = :staffId AND p.status = 'IN_PROGRESS' AND p.tenantId = :tenantId AND p.deleted = false")
    Optional<PatrolSession> findInProgressByStaffIdAndTenantId(@Param("staffId") UUID staffId, @Param("tenantId") UUID tenantId);

    @Query("SELECT AVG(p.completionPercentage) FROM PatrolSession p WHERE p.estateId = :estateId " +
            "AND p.status = 'COMPLETED' AND p.tenantId = :tenantId AND p.deleted = false " +
            "AND p.startedAt >= :from AND p.startedAt < :to")
    Double avgCompletionByEstateAndDateRange(@Param("estateId") UUID estateId, @Param("tenantId") UUID tenantId,
                                              @Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT COUNT(p) FROM PatrolSession p WHERE p.tenantId = :tenantId AND p.deleted = false")
    long countByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(p) FROM PatrolSession p WHERE p.status = :status AND p.tenantId = :tenantId AND p.deleted = false")
    long countByStatusAndTenantId(@Param("status") PatrolSessionStatus status, @Param("tenantId") UUID tenantId);
}
