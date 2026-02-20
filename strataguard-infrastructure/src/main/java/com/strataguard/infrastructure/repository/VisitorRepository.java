package com.strataguard.infrastructure.repository;

import com.strataguard.core.entity.Visitor;
import com.strataguard.core.enums.VisitorStatus;
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
public interface VisitorRepository extends JpaRepository<Visitor, UUID> {

    @Query("SELECT v FROM Visitor v WHERE v.tenantId = :tenantId AND v.deleted = false")
    Page<Visitor> findAllByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT v FROM Visitor v WHERE v.id = :id AND v.tenantId = :tenantId AND v.deleted = false")
    Optional<Visitor> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT v FROM Visitor v WHERE v.invitedBy = :invitedBy AND v.tenantId = :tenantId AND v.deleted = false")
    Page<Visitor> findByInvitedByAndTenantId(@Param("invitedBy") UUID invitedBy,
                                              @Param("tenantId") UUID tenantId,
                                              Pageable pageable);

    @Query("SELECT v FROM Visitor v WHERE v.status = :status AND v.tenantId = :tenantId AND v.deleted = false")
    Page<Visitor> findByStatusAndTenantId(@Param("status") VisitorStatus status,
                                          @Param("tenantId") UUID tenantId,
                                          Pageable pageable);

    @Query("SELECT v FROM Visitor v WHERE v.tenantId = :tenantId AND v.deleted = false " +
            "AND (LOWER(v.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(v.phone) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(v.email) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Visitor> search(@Param("query") String query,
                         @Param("tenantId") UUID tenantId,
                         Pageable pageable);

    @Query("SELECT v FROM Visitor v WHERE v.invitedBy = :invitedBy AND v.status IN :statuses " +
            "AND v.tenantId = :tenantId AND v.deleted = false")
    Page<Visitor> findByInvitedByAndStatusIn(@Param("invitedBy") UUID invitedBy,
                                             @Param("statuses") List<VisitorStatus> statuses,
                                             @Param("tenantId") UUID tenantId,
                                             Pageable pageable);

    @Query("SELECT v FROM Visitor v WHERE v.status = 'PENDING' AND v.tenantId = :tenantId AND v.deleted = false")
    Page<Visitor> findExpectedVisitors(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT COUNT(v) FROM Visitor v WHERE v.tenantId = :tenantId AND v.deleted = false")
    long countByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(v) FROM Visitor v WHERE v.status = :status AND v.tenantId = :tenantId AND v.deleted = false")
    long countByStatusAndTenantId(@Param("status") VisitorStatus status, @Param("tenantId") UUID tenantId);
}
