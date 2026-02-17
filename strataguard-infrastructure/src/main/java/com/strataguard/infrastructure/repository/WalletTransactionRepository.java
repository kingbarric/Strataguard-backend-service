package com.strataguard.infrastructure.repository;

import com.strataguard.core.entity.WalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, UUID> {

    @Query("SELECT wt FROM WalletTransaction wt WHERE wt.walletId = :walletId AND wt.tenantId = :tenantId " +
            "AND wt.deleted = false ORDER BY wt.createdAt DESC")
    Page<WalletTransaction> findByWalletIdAndTenantId(@Param("walletId") UUID walletId, @Param("tenantId") UUID tenantId, Pageable pageable);
}
