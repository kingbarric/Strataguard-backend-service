package com.strataguard.infrastructure.repository;

import com.strataguard.core.entity.ResidentWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResidentWalletRepository extends JpaRepository<ResidentWallet, UUID> {

    @Query("SELECT w FROM ResidentWallet w WHERE w.residentId = :residentId AND w.tenantId = :tenantId AND w.deleted = false")
    Optional<ResidentWallet> findByResidentIdAndTenantId(@Param("residentId") UUID residentId, @Param("tenantId") UUID tenantId);

    @Query("SELECT w FROM ResidentWallet w WHERE w.id = :id AND w.tenantId = :tenantId AND w.deleted = false")
    Optional<ResidentWallet> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);
}
