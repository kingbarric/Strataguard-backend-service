package com.strataguard.infrastructure.repository;

import com.strataguard.core.entity.EstateMembership;
import com.strataguard.core.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EstateMembershipRepository extends JpaRepository<EstateMembership, UUID> {

    @Query("SELECT m FROM EstateMembership m WHERE m.userId = :userId AND m.estateId = :estateId " +
           "AND m.status = 'ACTIVE' AND m.deleted = false")
    Optional<EstateMembership> findActiveByUserIdAndEstateId(
        @Param("userId") String userId, @Param("estateId") UUID estateId);

    @Query("SELECT m FROM EstateMembership m WHERE m.userId = :userId " +
           "AND m.status = 'ACTIVE' AND m.deleted = false AND m.tenantId = :tenantId")
    List<EstateMembership> findAllActiveByUserIdAndTenantId(
        @Param("userId") String userId, @Param("tenantId") UUID tenantId);

    @Query("SELECT m FROM EstateMembership m WHERE m.userId = :userId " +
           "AND m.status = 'ACTIVE' AND m.deleted = false")
    List<EstateMembership> findAllActiveByUserId(@Param("userId") String userId);

    @Query("SELECT m FROM EstateMembership m WHERE m.estateId = :estateId " +
           "AND m.tenantId = :tenantId AND m.deleted = false")
    Page<EstateMembership> findAllByEstateIdAndTenantId(
        @Param("estateId") UUID estateId, @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM EstateMembership m " +
           "WHERE m.userId = :userId AND m.estateId = :estateId " +
           "AND m.status = 'ACTIVE' AND m.deleted = false")
    boolean existsActiveByUserIdAndEstateId(
        @Param("userId") String userId, @Param("estateId") UUID estateId);

    @Query("SELECT m FROM EstateMembership m WHERE m.estateId = :estateId " +
           "AND m.role = :role AND m.status = 'ACTIVE' AND m.deleted = false AND m.tenantId = :tenantId")
    List<EstateMembership> findAllByEstateIdAndRoleAndTenantId(
        @Param("estateId") UUID estateId, @Param("role") UserRole role,
        @Param("tenantId") UUID tenantId);
}
