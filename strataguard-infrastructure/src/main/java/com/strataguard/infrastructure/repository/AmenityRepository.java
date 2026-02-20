package com.strataguard.infrastructure.repository;

import com.strataguard.core.entity.Amenity;
import com.strataguard.core.enums.AmenityStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AmenityRepository extends JpaRepository<Amenity, UUID> {

    @Query("SELECT a FROM Amenity a WHERE a.tenantId = :tenantId AND a.deleted = false")
    Page<Amenity> findAllByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT a FROM Amenity a WHERE a.id = :id AND a.tenantId = :tenantId AND a.deleted = false")
    Optional<Amenity> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT a FROM Amenity a WHERE a.estateId = :estateId AND a.tenantId = :tenantId AND a.deleted = false")
    Page<Amenity> findByEstateIdAndTenantId(@Param("estateId") UUID estateId, @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Amenity a " +
            "WHERE a.name = :name AND a.estateId = :estateId AND a.tenantId = :tenantId AND a.deleted = false")
    boolean existsByNameAndEstateIdAndTenantId(@Param("name") String name, @Param("estateId") UUID estateId, @Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(a) FROM Amenity a WHERE a.tenantId = :tenantId AND a.deleted = false")
    long countByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(a) FROM Amenity a WHERE a.tenantId = :tenantId AND a.status = :status AND a.deleted = false")
    long countByStatusAndTenantId(@Param("status") AmenityStatus status, @Param("tenantId") UUID tenantId);
}
