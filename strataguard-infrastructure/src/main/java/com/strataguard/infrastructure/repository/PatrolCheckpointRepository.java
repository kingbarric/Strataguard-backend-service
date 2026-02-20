package com.strataguard.infrastructure.repository;

import com.strataguard.core.entity.PatrolCheckpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatrolCheckpointRepository extends JpaRepository<PatrolCheckpoint, UUID> {

    @Query("SELECT c FROM PatrolCheckpoint c WHERE c.estateId = :estateId AND c.tenantId = :tenantId AND c.deleted = false ORDER BY c.sortOrder")
    List<PatrolCheckpoint> findByEstateIdAndTenantId(@Param("estateId") UUID estateId, @Param("tenantId") UUID tenantId);

    @Query("SELECT c FROM PatrolCheckpoint c WHERE c.id = :id AND c.tenantId = :tenantId AND c.deleted = false")
    Optional<PatrolCheckpoint> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT c FROM PatrolCheckpoint c WHERE c.qrCode = :qrCode AND c.tenantId = :tenantId AND c.deleted = false")
    Optional<PatrolCheckpoint> findByQrCodeAndTenantId(@Param("qrCode") String qrCode, @Param("tenantId") UUID tenantId);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM PatrolCheckpoint c " +
            "WHERE c.qrCode = :qrCode AND c.tenantId = :tenantId AND c.deleted = false")
    boolean existsByQrCodeAndTenantId(@Param("qrCode") String qrCode, @Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(c) FROM PatrolCheckpoint c WHERE c.estateId = :estateId AND c.tenantId = :tenantId AND c.active = true AND c.deleted = false")
    int countActiveByEstateIdAndTenantId(@Param("estateId") UUID estateId, @Param("tenantId") UUID tenantId);
}
