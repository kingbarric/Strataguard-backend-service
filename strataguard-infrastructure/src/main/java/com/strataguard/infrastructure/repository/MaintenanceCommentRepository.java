package com.strataguard.infrastructure.repository;

import com.strataguard.core.entity.MaintenanceComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MaintenanceCommentRepository extends JpaRepository<MaintenanceComment, UUID> {

    @Query("SELECT c FROM MaintenanceComment c WHERE c.requestId = :requestId AND c.tenantId = :tenantId AND c.deleted = false ORDER BY c.createdAt ASC")
    Page<MaintenanceComment> findByRequestIdAndTenantId(@Param("requestId") UUID requestId, @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT c FROM MaintenanceComment c WHERE c.requestId = :requestId AND c.tenantId = :tenantId " +
            "AND c.internal = false AND c.deleted = false ORDER BY c.createdAt ASC")
    Page<MaintenanceComment> findPublicByRequestIdAndTenantId(@Param("requestId") UUID requestId, @Param("tenantId") UUID tenantId, Pageable pageable);
}
