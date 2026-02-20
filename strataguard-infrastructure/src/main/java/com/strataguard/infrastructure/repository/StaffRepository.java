package com.strataguard.infrastructure.repository;

import com.strataguard.core.entity.Staff;
import com.strataguard.core.enums.StaffDepartment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StaffRepository extends JpaRepository<Staff, UUID> {

    @Query("SELECT s FROM Staff s WHERE s.tenantId = :tenantId AND s.deleted = false")
    Page<Staff> findAllByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT s FROM Staff s WHERE s.id = :id AND s.tenantId = :tenantId AND s.deleted = false")
    Optional<Staff> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT s FROM Staff s WHERE s.estateId = :estateId AND s.tenantId = :tenantId AND s.deleted = false")
    Page<Staff> findByEstateIdAndTenantId(@Param("estateId") UUID estateId, @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT s FROM Staff s WHERE s.department = :department AND s.tenantId = :tenantId AND s.deleted = false")
    Page<Staff> findByDepartmentAndTenantId(@Param("department") StaffDepartment department, @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT s FROM Staff s WHERE s.userId = :userId AND s.tenantId = :tenantId AND s.deleted = false")
    Optional<Staff> findByUserIdAndTenantId(@Param("userId") String userId, @Param("tenantId") UUID tenantId);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Staff s " +
            "WHERE s.badgeNumber = :badgeNumber AND s.tenantId = :tenantId AND s.deleted = false")
    boolean existsByBadgeNumberAndTenantId(@Param("badgeNumber") String badgeNumber, @Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(s) FROM Staff s WHERE s.tenantId = :tenantId AND s.deleted = false")
    long countByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(s) FROM Staff s WHERE s.department = :department AND s.estateId = :estateId AND s.tenantId = :tenantId AND s.deleted = false AND s.active = true")
    long countByDepartmentAndEstateIdAndTenantId(@Param("department") StaffDepartment department, @Param("estateId") UUID estateId, @Param("tenantId") UUID tenantId);
}
