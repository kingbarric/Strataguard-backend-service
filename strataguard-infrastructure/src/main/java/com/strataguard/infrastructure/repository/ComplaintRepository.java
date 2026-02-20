package com.strataguard.infrastructure.repository;

import com.strataguard.core.entity.Complaint;
import com.strataguard.core.enums.ComplaintCategory;
import com.strataguard.core.enums.ComplaintStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, UUID> {

    @Query("SELECT c FROM Complaint c WHERE c.tenantId = :tenantId AND c.deleted = false ORDER BY c.createdAt DESC")
    Page<Complaint> findAllByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT c FROM Complaint c WHERE c.id = :id AND c.tenantId = :tenantId AND c.deleted = false")
    Optional<Complaint> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT c FROM Complaint c WHERE c.estateId = :estateId AND c.tenantId = :tenantId AND c.deleted = false ORDER BY c.createdAt DESC")
    Page<Complaint> findByEstateIdAndTenantId(@Param("estateId") UUID estateId, @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT c FROM Complaint c WHERE c.residentId = :residentId AND c.tenantId = :tenantId AND c.deleted = false ORDER BY c.createdAt DESC")
    Page<Complaint> findByResidentIdAndTenantId(@Param("residentId") UUID residentId, @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT c FROM Complaint c WHERE c.status = :status AND c.tenantId = :tenantId AND c.deleted = false ORDER BY c.createdAt DESC")
    Page<Complaint> findByStatusAndTenantId(@Param("status") ComplaintStatus status, @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT c FROM Complaint c WHERE c.category = :category AND c.tenantId = :tenantId AND c.deleted = false ORDER BY c.createdAt DESC")
    Page<Complaint> findByCategoryAndTenantId(@Param("category") ComplaintCategory category, @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT COUNT(c) FROM Complaint c WHERE c.tenantId = :tenantId AND c.deleted = false")
    long countByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(c) FROM Complaint c WHERE c.status = :status AND c.tenantId = :tenantId AND c.deleted = false")
    long countByStatusAndTenantId(@Param("status") ComplaintStatus status, @Param("tenantId") UUID tenantId);
}
