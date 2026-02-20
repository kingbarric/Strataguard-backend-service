package com.strataguard.infrastructure.repository;

import com.strataguard.core.entity.AuditLog;
import com.strataguard.core.enums.AuditAction;
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
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    @Query("SELECT a FROM AuditLog a WHERE a.tenantId = :tenantId ORDER BY a.timestamp DESC")
    Page<AuditLog> findAllByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.id = :id AND a.tenantId = :tenantId")
    Optional<AuditLog> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT a FROM AuditLog a WHERE a.entityType = :entityType AND a.entityId = :entityId AND a.tenantId = :tenantId ORDER BY a.timestamp DESC")
    Page<AuditLog> findByEntityTypeAndEntityId(@Param("entityType") String entityType, @Param("entityId") String entityId,
                                                @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.actorId = :actorId AND a.tenantId = :tenantId ORDER BY a.timestamp DESC")
    Page<AuditLog> findByActorId(@Param("actorId") String actorId, @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.action = :action AND a.tenantId = :tenantId ORDER BY a.timestamp DESC")
    Page<AuditLog> findByAction(@Param("action") AuditAction action, @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.entityType = :entityType AND a.tenantId = :tenantId ORDER BY a.timestamp DESC")
    Page<AuditLog> findByEntityType(@Param("entityType") String entityType, @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.tenantId = :tenantId AND a.timestamp >= :start AND a.timestamp <= :end ORDER BY a.timestamp DESC")
    Page<AuditLog> findByDateRange(@Param("tenantId") UUID tenantId, @Param("start") Instant start,
                                    @Param("end") Instant end, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.tenantId = :tenantId ORDER BY a.timestamp DESC")
    Page<AuditLog> findLatest(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.tenantId = :tenantId")
    long countByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.action = :action AND a.tenantId = :tenantId")
    long countByActionAndTenantId(@Param("action") AuditAction action, @Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.entityType = :entityType AND a.tenantId = :tenantId")
    long countByEntityTypeAndTenantId(@Param("entityType") String entityType, @Param("tenantId") UUID tenantId);

    @Query(value = "SELECT * FROM audit_logs WHERE tenant_id = :tenantId ORDER BY timestamp DESC LIMIT 1", nativeQuery = true)
    Optional<AuditLog> findLastByTenantId(@Param("tenantId") UUID tenantId);
}
