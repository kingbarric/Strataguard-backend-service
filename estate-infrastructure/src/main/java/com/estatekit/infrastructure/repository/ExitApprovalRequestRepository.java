package com.estatekit.infrastructure.repository;

import com.estatekit.core.entity.ExitApprovalRequest;
import com.estatekit.core.enums.ExitApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExitApprovalRequestRepository extends JpaRepository<ExitApprovalRequest, UUID> {

    @Query("SELECT r FROM ExitApprovalRequest r WHERE r.id = :id AND r.tenantId = :tenantId AND r.deleted = false")
    Optional<ExitApprovalRequest> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT r FROM ExitApprovalRequest r WHERE r.residentId = :residentId AND r.status = :status AND r.tenantId = :tenantId AND r.deleted = false ORDER BY r.createdAt DESC")
    List<ExitApprovalRequest> findByResidentIdAndStatusAndTenantId(@Param("residentId") UUID residentId,
                                                                    @Param("status") ExitApprovalStatus status,
                                                                    @Param("tenantId") UUID tenantId);

    @Query("SELECT r FROM ExitApprovalRequest r WHERE r.sessionId = :sessionId AND r.status = :status AND r.tenantId = :tenantId AND r.deleted = false")
    Optional<ExitApprovalRequest> findBySessionIdAndStatusAndTenantId(@Param("sessionId") UUID sessionId,
                                                                      @Param("status") ExitApprovalStatus status,
                                                                      @Param("tenantId") UUID tenantId);
}
