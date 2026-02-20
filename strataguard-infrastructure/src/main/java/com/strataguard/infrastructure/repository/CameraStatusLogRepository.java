package com.strataguard.infrastructure.repository;

import com.strataguard.core.entity.CameraStatusLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CameraStatusLogRepository extends JpaRepository<CameraStatusLog, UUID> {

    @Query("SELECT l FROM CameraStatusLog l WHERE l.cameraId = :cameraId AND l.tenantId = :tenantId AND l.deleted = false ORDER BY l.changedAt DESC")
    Page<CameraStatusLog> findByCameraIdAndTenantId(@Param("cameraId") UUID cameraId, @Param("tenantId") UUID tenantId, Pageable pageable);
}
