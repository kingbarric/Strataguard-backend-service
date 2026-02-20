package com.strataguard.infrastructure.repository;

import com.strataguard.core.entity.Resident;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResidentRepository extends JpaRepository<Resident, UUID> {

    @Query("SELECT r FROM Resident r WHERE r.tenantId = :tenantId AND r.deleted = false")
    Page<Resident> findAllByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT r FROM Resident r WHERE r.id = :id AND r.tenantId = :tenantId AND r.deleted = false")
    Optional<Resident> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT r FROM Resident r WHERE r.userId = :userId AND r.tenantId = :tenantId AND r.deleted = false")
    Optional<Resident> findByUserIdAndTenantId(@Param("userId") String userId, @Param("tenantId") UUID tenantId);

    @Query("SELECT r FROM Resident r WHERE r.email = :email AND r.tenantId = :tenantId AND r.deleted = false")
    Optional<Resident> findByEmailAndTenantId(@Param("email") String email, @Param("tenantId") UUID tenantId);

    @Query("SELECT r FROM Resident r WHERE r.tenantId = :tenantId AND r.deleted = false " +
            "AND (LOWER(r.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(r.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(r.email) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(r.phone) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Resident> searchByTenantId(@Param("tenantId") UUID tenantId, @Param("search") String search, Pageable pageable);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Resident r " +
            "WHERE r.email = :email AND r.tenantId = :tenantId AND r.deleted = false")
    boolean existsByEmailAndTenantId(@Param("email") String email, @Param("tenantId") UUID tenantId);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Resident r " +
            "WHERE r.userId = :userId AND r.tenantId = :tenantId AND r.deleted = false")
    boolean existsByUserIdAndTenantId(@Param("userId") String userId, @Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(r) FROM Resident r WHERE r.tenantId = :tenantId AND r.deleted = false")
    long countByTenantId(@Param("tenantId") UUID tenantId);
}
