package com.strataguard.infrastructure.repository;

import com.strataguard.core.entity.CctvCamera;
import com.strataguard.core.enums.CameraStatus;
import com.strataguard.core.enums.CameraZone;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CctvCameraRepository extends JpaRepository<CctvCamera, UUID> {

    @Query("SELECT c FROM CctvCamera c WHERE c.id = :id AND c.tenantId = :tenantId AND c.deleted = false")
    Optional<CctvCamera> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT c FROM CctvCamera c WHERE c.estateId = :estateId AND c.tenantId = :tenantId AND c.deleted = false")
    Page<CctvCamera> findByEstateIdAndTenantId(@Param("estateId") UUID estateId, @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT c FROM CctvCamera c WHERE c.zone = :zone AND c.estateId = :estateId AND c.tenantId = :tenantId AND c.deleted = false")
    Page<CctvCamera> findByZoneAndEstateIdAndTenantId(@Param("zone") CameraZone zone, @Param("estateId") UUID estateId,
                                                       @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM CctvCamera c " +
            "WHERE c.cameraCode = :cameraCode AND c.tenantId = :tenantId AND c.deleted = false")
    boolean existsByCameraCodeAndTenantId(@Param("cameraCode") String cameraCode, @Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(c) FROM CctvCamera c WHERE c.status = :status AND c.tenantId = :tenantId AND c.deleted = false")
    long countByStatusAndTenantId(@Param("status") CameraStatus status, @Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(c) FROM CctvCamera c WHERE c.tenantId = :tenantId AND c.deleted = false")
    long countByTenantId(@Param("tenantId") UUID tenantId);
}
