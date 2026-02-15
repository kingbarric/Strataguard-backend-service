package com.estatekit.infrastructure.repository;

import com.estatekit.core.entity.Estate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EstateRepository extends JpaRepository<Estate, UUID> {

    @Query("SELECT e FROM Estate e WHERE e.tenantId = :tenantId AND e.deleted = false")
    Page<Estate> findAllByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT e FROM Estate e WHERE e.id = :id AND e.tenantId = :tenantId AND e.deleted = false")
    Optional<Estate> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM Estate e " +
            "WHERE e.name = :name AND e.tenantId = :tenantId AND e.deleted = false")
    boolean existsByNameAndTenantId(@Param("name") String name, @Param("tenantId") UUID tenantId);

    @Query("SELECT e FROM Estate e WHERE e.tenantId = :tenantId AND e.deleted = false " +
            "AND (LOWER(e.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(e.address) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(e.city) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Estate> searchByTenantId(@Param("tenantId") UUID tenantId, @Param("search") String search, Pageable pageable);
}
