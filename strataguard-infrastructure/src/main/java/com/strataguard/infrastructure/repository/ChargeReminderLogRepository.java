package com.strataguard.infrastructure.repository;

import com.strataguard.core.entity.ChargeReminderLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChargeReminderLogRepository extends JpaRepository<ChargeReminderLog, UUID> {

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM ChargeReminderLog r " +
            "WHERE r.invoiceId = :invoiceId AND r.daysBefore = :daysBefore AND r.tenantId = :tenantId AND r.deleted = false")
    boolean existsByInvoiceIdAndDaysBeforeAndTenantId(@Param("invoiceId") UUID invoiceId,
                                                       @Param("daysBefore") int daysBefore,
                                                       @Param("tenantId") UUID tenantId);

    @Query("SELECT r FROM ChargeReminderLog r WHERE r.invoiceId = :invoiceId AND r.tenantId = :tenantId AND r.deleted = false")
    List<ChargeReminderLog> findByInvoiceIdAndTenantId(@Param("invoiceId") UUID invoiceId, @Param("tenantId") UUID tenantId);
}
