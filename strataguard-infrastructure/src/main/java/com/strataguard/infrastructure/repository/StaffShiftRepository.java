package com.strataguard.infrastructure.repository;

import com.strataguard.core.entity.StaffShift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StaffShiftRepository extends JpaRepository<StaffShift, UUID> {

    @Query("SELECT s FROM StaffShift s WHERE s.staffId = :staffId AND s.tenantId = :tenantId AND s.deleted = false")
    List<StaffShift> findByStaffIdAndTenantId(@Param("staffId") UUID staffId, @Param("tenantId") UUID tenantId);

    @Query("SELECT s FROM StaffShift s WHERE s.id = :id AND s.tenantId = :tenantId AND s.deleted = false")
    Optional<StaffShift> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT s FROM StaffShift s WHERE s.estateId = :estateId AND s.tenantId = :tenantId " +
            "AND s.active = true AND s.deleted = false " +
            "AND (s.effectiveTo IS NULL OR s.effectiveTo >= CURRENT_DATE) " +
            "AND s.effectiveFrom <= CURRENT_DATE")
    List<StaffShift> findActiveByEstateIdAndTenantId(@Param("estateId") UUID estateId, @Param("tenantId") UUID tenantId);
}
