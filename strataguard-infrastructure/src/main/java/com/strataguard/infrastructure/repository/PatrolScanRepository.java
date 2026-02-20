package com.strataguard.infrastructure.repository;

import com.strataguard.core.entity.PatrolScan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PatrolScanRepository extends JpaRepository<PatrolScan, UUID> {

    @Query("SELECT s FROM PatrolScan s WHERE s.sessionId = :sessionId AND s.tenantId = :tenantId AND s.deleted = false ORDER BY s.scannedAt")
    List<PatrolScan> findBySessionIdAndTenantId(@Param("sessionId") UUID sessionId, @Param("tenantId") UUID tenantId);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM PatrolScan s " +
            "WHERE s.sessionId = :sessionId AND s.checkpointId = :checkpointId AND s.tenantId = :tenantId AND s.deleted = false")
    boolean existsBySessionIdAndCheckpointIdAndTenantId(@Param("sessionId") UUID sessionId,
                                                         @Param("checkpointId") UUID checkpointId,
                                                         @Param("tenantId") UUID tenantId);
}
