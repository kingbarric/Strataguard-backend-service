package com.strataguard.infrastructure.repository;

import com.strataguard.core.entity.UtilityReading;
import com.strataguard.core.enums.UtilityReadingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UtilityReadingRepository extends JpaRepository<UtilityReading, UUID> {

    @Query("SELECT r FROM UtilityReading r WHERE r.id = :id AND r.tenantId = :tenantId AND r.deleted = false")
    Optional<UtilityReading> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT r FROM UtilityReading r WHERE r.meterId = :meterId AND r.tenantId = :tenantId AND r.deleted = false ORDER BY r.readingDate DESC")
    Page<UtilityReading> findByMeterIdAndTenantId(@Param("meterId") UUID meterId, @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT r FROM UtilityReading r WHERE r.unitId = :unitId AND r.tenantId = :tenantId " +
            "AND r.readingDate >= :periodStart AND r.readingDate <= :periodEnd AND r.deleted = false ORDER BY r.readingDate DESC")
    List<UtilityReading> findByUnitIdAndPeriodAndTenantId(@Param("unitId") UUID unitId,
                                                          @Param("periodStart") LocalDate periodStart,
                                                          @Param("periodEnd") LocalDate periodEnd,
                                                          @Param("tenantId") UUID tenantId);

    @Query("SELECT r FROM UtilityReading r WHERE r.status = 'VALIDATED' AND r.invoiceId IS NULL AND r.tenantId = :tenantId AND r.deleted = false")
    List<UtilityReading> findUninvoicedValidatedReadings(@Param("tenantId") UUID tenantId);

    @Query("SELECT r FROM UtilityReading r WHERE r.meterId = :meterId AND r.readingDate >= :sinceDate " +
            "AND r.tenantId = :tenantId AND r.deleted = false ORDER BY r.readingDate ASC")
    List<UtilityReading> findByMeterIdSinceDateAndTenantId(@Param("meterId") UUID meterId,
                                                            @Param("sinceDate") LocalDate sinceDate,
                                                            @Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(r) FROM UtilityReading r WHERE r.status = :status AND r.tenantId = :tenantId AND r.deleted = false")
    long countByStatusAndTenantId(@Param("status") UtilityReadingStatus status, @Param("tenantId") UUID tenantId);

    @Query("SELECT COALESCE(SUM(r.cost), 0) FROM UtilityReading r WHERE r.tenantId = :tenantId AND r.deleted = false")
    BigDecimal sumCostByTenantId(@Param("tenantId") UUID tenantId);
}
