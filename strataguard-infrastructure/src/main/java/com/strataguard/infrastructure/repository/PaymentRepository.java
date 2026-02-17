package com.strataguard.infrastructure.repository;

import com.strataguard.core.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    @Query("SELECT p FROM Payment p WHERE p.tenantId = :tenantId AND p.deleted = false")
    Page<Payment> findAllByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT p FROM Payment p WHERE p.id = :id AND p.tenantId = :tenantId AND p.deleted = false")
    Optional<Payment> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT p FROM Payment p WHERE p.invoiceId = :invoiceId AND p.tenantId = :tenantId AND p.deleted = false")
    Page<Payment> findByInvoiceIdAndTenantId(@Param("invoiceId") UUID invoiceId, @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT p FROM Payment p WHERE p.reference = :reference AND p.tenantId = :tenantId AND p.deleted = false")
    Optional<Payment> findByReferenceAndTenantId(@Param("reference") String reference, @Param("tenantId") UUID tenantId);

    @Query("SELECT p FROM Payment p WHERE p.reference = :reference AND p.deleted = false")
    Optional<Payment> findByReference(@Param("reference") String reference);
}
