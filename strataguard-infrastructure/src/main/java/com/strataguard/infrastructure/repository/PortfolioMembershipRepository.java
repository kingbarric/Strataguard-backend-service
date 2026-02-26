package com.strataguard.infrastructure.repository;

import com.strataguard.core.entity.PortfolioMembership;
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
public interface PortfolioMembershipRepository extends JpaRepository<PortfolioMembership, UUID> {

    @Query("SELECT pm FROM PortfolioMembership pm WHERE pm.userId = :userId AND pm.portfolioId = :portfolioId " +
           "AND pm.status = com.strataguard.core.enums.MembershipStatus.ACTIVE AND pm.deleted = false")
    Optional<PortfolioMembership> findActiveByUserIdAndPortfolioId(
            @Param("userId") String userId, @Param("portfolioId") UUID portfolioId);

    @Query("SELECT pm FROM PortfolioMembership pm WHERE pm.userId = :userId " +
           "AND pm.status = com.strataguard.core.enums.MembershipStatus.ACTIVE AND pm.deleted = false")
    List<PortfolioMembership> findAllActiveByUserId(@Param("userId") String userId);

    @Query("SELECT pm FROM PortfolioMembership pm WHERE pm.portfolioId = :portfolioId " +
           "AND pm.tenantId = :tenantId AND pm.deleted = false")
    Page<PortfolioMembership> findAllByPortfolioIdAndTenantId(
            @Param("portfolioId") UUID portfolioId, @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT CASE WHEN COUNT(pm) > 0 THEN true ELSE false END FROM PortfolioMembership pm " +
           "WHERE pm.userId = :userId AND pm.portfolioId = :portfolioId " +
           "AND pm.status = com.strataguard.core.enums.MembershipStatus.ACTIVE AND pm.deleted = false")
    boolean existsActiveByUserIdAndPortfolioId(
            @Param("userId") String userId, @Param("portfolioId") UUID portfolioId);
}
