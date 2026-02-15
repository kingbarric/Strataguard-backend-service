package com.estatekit.infrastructure.repository;

import com.estatekit.core.entity.GateAccessLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface GateAccessLogRepository extends JpaRepository<GateAccessLog, UUID> {

    @Query("SELECT l FROM GateAccessLog l WHERE l.tenantId = :tenantId AND l.deleted = false ORDER BY l.createdAt DESC")
    Page<GateAccessLog> findAllByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT l FROM GateAccessLog l WHERE l.sessionId = :sessionId AND l.tenantId = :tenantId AND l.deleted = false ORDER BY l.createdAt DESC")
    Page<GateAccessLog> findBySessionIdAndTenantId(@Param("sessionId") UUID sessionId,
                                                    @Param("tenantId") UUID tenantId,
                                                    Pageable pageable);

    @Query("SELECT l FROM GateAccessLog l WHERE l.vehicleId = :vehicleId AND l.tenantId = :tenantId AND l.deleted = false ORDER BY l.createdAt DESC")
    Page<GateAccessLog> findByVehicleIdAndTenantId(@Param("vehicleId") UUID vehicleId,
                                                    @Param("tenantId") UUID tenantId,
                                                    Pageable pageable);
}
