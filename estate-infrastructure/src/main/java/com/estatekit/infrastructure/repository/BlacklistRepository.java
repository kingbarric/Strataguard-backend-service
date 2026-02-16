package com.estatekit.infrastructure.repository;

import com.estatekit.core.entity.Blacklist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BlacklistRepository extends JpaRepository<Blacklist, UUID> {

    @Query("SELECT b FROM Blacklist b WHERE b.tenantId = :tenantId AND b.deleted = false")
    Page<Blacklist> findAllByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT b FROM Blacklist b WHERE b.id = :id AND b.tenantId = :tenantId AND b.deleted = false")
    Optional<Blacklist> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT b FROM Blacklist b WHERE b.plateNumber = :plateNumber AND b.active = true " +
            "AND b.tenantId = :tenantId AND b.deleted = false")
    Optional<Blacklist> findByPlateNumberAndTenantId(@Param("plateNumber") String plateNumber,
                                                     @Param("tenantId") UUID tenantId);

    @Query("SELECT b FROM Blacklist b WHERE b.phone = :phone AND b.active = true " +
            "AND b.tenantId = :tenantId AND b.deleted = false")
    Optional<Blacklist> findByPhoneAndTenantId(@Param("phone") String phone,
                                               @Param("tenantId") UUID tenantId);

    @Query("SELECT b FROM Blacklist b WHERE b.tenantId = :tenantId AND b.deleted = false " +
            "AND (LOWER(b.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(b.phone) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(b.plateNumber) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Blacklist> search(@Param("query") String query,
                           @Param("tenantId") UUID tenantId,
                           Pageable pageable);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Blacklist b " +
            "WHERE b.plateNumber = :plateNumber AND b.active = true " +
            "AND b.tenantId = :tenantId AND b.deleted = false")
    boolean isPlateBlacklisted(@Param("plateNumber") String plateNumber,
                               @Param("tenantId") UUID tenantId);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Blacklist b " +
            "WHERE b.phone = :phone AND b.active = true " +
            "AND b.tenantId = :tenantId AND b.deleted = false")
    boolean isPhoneBlacklisted(@Param("phone") String phone,
                               @Param("tenantId") UUID tenantId);
}
