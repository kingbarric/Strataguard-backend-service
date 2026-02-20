package com.strataguard.infrastructure.repository;

import com.strataguard.core.entity.Poll;
import com.strataguard.core.enums.PollStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PollRepository extends JpaRepository<Poll, UUID> {

    @Query("SELECT p FROM Poll p WHERE p.tenantId = :tenantId AND p.deleted = false ORDER BY p.createdAt DESC")
    Page<Poll> findAllByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT p FROM Poll p WHERE p.id = :id AND p.tenantId = :tenantId AND p.deleted = false")
    Optional<Poll> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT p FROM Poll p WHERE p.estateId = :estateId AND p.tenantId = :tenantId AND p.deleted = false ORDER BY p.createdAt DESC")
    Page<Poll> findByEstateIdAndTenantId(@Param("estateId") UUID estateId, @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT p FROM Poll p WHERE p.estateId = :estateId AND p.status = :status AND p.tenantId = :tenantId AND p.deleted = false ORDER BY p.deadline ASC")
    Page<Poll> findByEstateIdAndStatusAndTenantId(@Param("estateId") UUID estateId, @Param("status") PollStatus status,
                                                   @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT COUNT(p) FROM Poll p WHERE p.tenantId = :tenantId AND p.deleted = false")
    long countByTenantId(@Param("tenantId") UUID tenantId);
}
