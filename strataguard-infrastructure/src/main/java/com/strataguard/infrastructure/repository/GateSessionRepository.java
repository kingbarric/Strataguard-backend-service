package com.strataguard.infrastructure.repository;

import com.strataguard.core.entity.GateSession;
import com.strataguard.core.enums.GateSessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GateSessionRepository extends JpaRepository<GateSession, UUID> {

    @Query("SELECT s FROM GateSession s WHERE s.tenantId = :tenantId AND s.deleted = false")
    Page<GateSession> findAllByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT s FROM GateSession s WHERE s.id = :id AND s.tenantId = :tenantId AND s.deleted = false")
    Optional<GateSession> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT s FROM GateSession s WHERE s.vehicleId = :vehicleId AND s.status = :status AND s.tenantId = :tenantId AND s.deleted = false")
    Optional<GateSession> findByVehicleIdAndStatusAndTenantId(@Param("vehicleId") UUID vehicleId,
                                                              @Param("status") GateSessionStatus status,
                                                              @Param("tenantId") UUID tenantId);

    @Query("SELECT s FROM GateSession s WHERE s.vehicleId = :vehicleId AND s.tenantId = :tenantId AND s.deleted = false")
    Page<GateSession> findByVehicleIdAndTenantId(@Param("vehicleId") UUID vehicleId,
                                                  @Param("tenantId") UUID tenantId,
                                                  Pageable pageable);

    @Query("SELECT s FROM GateSession s WHERE s.residentId = :residentId AND s.tenantId = :tenantId AND s.deleted = false")
    Page<GateSession> findByResidentIdAndTenantId(@Param("residentId") UUID residentId,
                                                   @Param("tenantId") UUID tenantId,
                                                   Pageable pageable);

    @Query("SELECT s FROM GateSession s WHERE s.status = :status AND s.tenantId = :tenantId AND s.deleted = false")
    Page<GateSession> findByStatusAndTenantId(@Param("status") GateSessionStatus status,
                                              @Param("tenantId") UUID tenantId,
                                              Pageable pageable);

    @Query("SELECT COUNT(s) FROM GateSession s WHERE s.tenantId = :tenantId AND s.deleted = false")
    long countByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(s) FROM GateSession s WHERE s.status = :status AND s.tenantId = :tenantId AND s.deleted = false")
    long countByStatusAndTenantId(@Param("status") GateSessionStatus status, @Param("tenantId") UUID tenantId);
}
