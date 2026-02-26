package com.strataguard.infrastructure.repository;

import com.strataguard.core.entity.Portfolio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, UUID> {

    @Query("SELECT p FROM Portfolio p WHERE p.id = :id AND p.tenantId = :tenantId AND p.deleted = false")
    Optional<Portfolio> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT p FROM Portfolio p WHERE p.tenantId = :tenantId AND p.deleted = false")
    Page<Portfolio> findAllByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Portfolio p " +
           "WHERE p.name = :name AND p.tenantId = :tenantId AND p.deleted = false")
    boolean existsByNameAndTenantId(@Param("name") String name, @Param("tenantId") UUID tenantId);
}
