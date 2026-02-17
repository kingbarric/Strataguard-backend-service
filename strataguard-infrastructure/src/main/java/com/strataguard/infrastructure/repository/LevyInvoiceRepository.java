package com.strataguard.infrastructure.repository;

import com.strataguard.core.entity.LevyInvoice;
import com.strataguard.core.enums.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LevyInvoiceRepository extends JpaRepository<LevyInvoice, UUID> {

    @Query("SELECT i FROM LevyInvoice i WHERE i.tenantId = :tenantId AND i.deleted = false")
    Page<LevyInvoice> findAllByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT i FROM LevyInvoice i WHERE i.id = :id AND i.tenantId = :tenantId AND i.deleted = false")
    Optional<LevyInvoice> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT i FROM LevyInvoice i WHERE i.unitId = :unitId AND i.tenantId = :tenantId AND i.deleted = false")
    Page<LevyInvoice> findByUnitIdAndTenantId(@Param("unitId") UUID unitId, @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT i FROM LevyInvoice i WHERE i.residentId = :residentId AND i.tenantId = :tenantId AND i.deleted = false")
    Page<LevyInvoice> findByResidentIdAndTenantId(@Param("residentId") UUID residentId, @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT i FROM LevyInvoice i WHERE i.status = :status AND i.tenantId = :tenantId AND i.deleted = false")
    Page<LevyInvoice> findByStatusAndTenantId(@Param("status") InvoiceStatus status, @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT i FROM LevyInvoice i WHERE i.tenantId = :tenantId AND i.deleted = false " +
            "AND i.status IN ('PENDING', 'PARTIAL') AND i.dueDate < :today")
    List<LevyInvoice> findOverdueByTenantId(@Param("tenantId") UUID tenantId, @Param("today") LocalDate today);

    @Query("SELECT i FROM LevyInvoice i WHERE i.tenantId = :tenantId AND i.deleted = false " +
            "AND i.status = 'OVERDUE'")
    Page<LevyInvoice> findOverduePagedByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END FROM LevyInvoice i " +
            "WHERE i.invoiceNumber = :invoiceNumber AND i.tenantId = :tenantId AND i.deleted = false")
    boolean existsByInvoiceNumberAndTenantId(@Param("invoiceNumber") String invoiceNumber, @Param("tenantId") UUID tenantId);

    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END FROM LevyInvoice i " +
            "WHERE i.levyTypeId = :levyTypeId AND i.unitId = :unitId AND i.tenantId = :tenantId " +
            "AND i.status IN ('PENDING', 'PARTIAL') AND i.billingPeriodStart = :periodStart " +
            "AND i.billingPeriodEnd = :periodEnd AND i.deleted = false")
    boolean existsActiveInvoice(@Param("levyTypeId") UUID levyTypeId, @Param("unitId") UUID unitId,
                                @Param("tenantId") UUID tenantId, @Param("periodStart") LocalDate periodStart,
                                @Param("periodEnd") LocalDate periodEnd);

    @Query("SELECT COUNT(i) FROM LevyInvoice i WHERE i.tenantId = :tenantId AND i.deleted = false " +
            "AND i.invoiceNumber LIKE :prefix")
    long countByInvoiceNumberPrefix(@Param("tenantId") UUID tenantId, @Param("prefix") String prefix);

    @Query("SELECT i FROM LevyInvoice i WHERE i.tenantId = :tenantId AND i.deleted = false " +
            "AND (LOWER(i.invoiceNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(i.notes) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<LevyInvoice> search(@Param("tenantId") UUID tenantId, @Param("search") String search, Pageable pageable);

    @Query("SELECT COUNT(i) FROM LevyInvoice i WHERE i.tenantId = :tenantId AND i.deleted = false")
    long countByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT COALESCE(SUM(i.totalAmount), 0) FROM LevyInvoice i WHERE i.tenantId = :tenantId AND i.deleted = false")
    java.math.BigDecimal sumTotalAmountByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT COALESCE(SUM(i.paidAmount), 0) FROM LevyInvoice i WHERE i.tenantId = :tenantId AND i.deleted = false")
    java.math.BigDecimal sumPaidAmountByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT COALESCE(SUM(i.totalAmount - i.paidAmount), 0) FROM LevyInvoice i WHERE i.tenantId = :tenantId " +
            "AND i.status = 'PENDING' AND i.deleted = false")
    java.math.BigDecimal sumPendingAmountByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT COALESCE(SUM(i.totalAmount - i.paidAmount), 0) FROM LevyInvoice i WHERE i.tenantId = :tenantId " +
            "AND i.status = 'OVERDUE' AND i.deleted = false")
    java.math.BigDecimal sumOverdueAmountByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(i) FROM LevyInvoice i WHERE i.tenantId = :tenantId AND i.status = 'OVERDUE' AND i.deleted = false")
    long countOverdueByTenantId(@Param("tenantId") UUID tenantId);
}
