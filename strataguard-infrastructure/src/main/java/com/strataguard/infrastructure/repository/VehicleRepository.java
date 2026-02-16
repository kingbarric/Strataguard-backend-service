package com.strataguard.infrastructure.repository;

import com.strataguard.core.entity.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {

    @Query("SELECT v FROM Vehicle v WHERE v.tenantId = :tenantId AND v.deleted = false")
    Page<Vehicle> findAllByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT v FROM Vehicle v WHERE v.id = :id AND v.tenantId = :tenantId AND v.deleted = false")
    Optional<Vehicle> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT v FROM Vehicle v WHERE v.residentId = :residentId AND v.tenantId = :tenantId AND v.deleted = false")
    Page<Vehicle> findByResidentIdAndTenantId(@Param("residentId") UUID residentId,
                                               @Param("tenantId") UUID tenantId,
                                               Pageable pageable);

    @Query("SELECT v FROM Vehicle v WHERE v.plateNumber = :plateNumber AND v.tenantId = :tenantId AND v.deleted = false")
    Optional<Vehicle> findByPlateNumberAndTenantId(@Param("plateNumber") String plateNumber,
                                                    @Param("tenantId") UUID tenantId);

    @Query("SELECT v FROM Vehicle v WHERE v.tenantId = :tenantId AND v.deleted = false " +
            "AND (LOWER(v.plateNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(v.make) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(v.model) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(v.color) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Vehicle> searchByTenantId(@Param("tenantId") UUID tenantId, @Param("search") String search, Pageable pageable);

    @Query("SELECT CASE WHEN COUNT(v) > 0 THEN true ELSE false END FROM Vehicle v " +
            "WHERE v.plateNumber = :plateNumber AND v.tenantId = :tenantId AND v.deleted = false")
    boolean existsByPlateNumberAndTenantId(@Param("plateNumber") String plateNumber,
                                            @Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(v) FROM Vehicle v WHERE v.residentId = :residentId AND v.tenantId = :tenantId AND v.deleted = false")
    long countByResidentIdAndTenantId(@Param("residentId") UUID residentId, @Param("tenantId") UUID tenantId);

    @Query("SELECT v FROM Vehicle v WHERE v.qrStickerCode = :qrStickerCode AND v.tenantId = :tenantId AND v.deleted = false")
    Optional<Vehicle> findByQrStickerCodeAndTenantId(@Param("qrStickerCode") String qrStickerCode,
                                                      @Param("tenantId") UUID tenantId);
}
