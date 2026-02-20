package com.strataguard.infrastructure.repository;

import com.strataguard.core.entity.PollOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PollOptionRepository extends JpaRepository<PollOption, UUID> {

    @Query("SELECT o FROM PollOption o WHERE o.pollId = :pollId AND o.tenantId = :tenantId AND o.deleted = false ORDER BY o.displayOrder ASC")
    List<PollOption> findByPollIdAndTenantId(@Param("pollId") UUID pollId, @Param("tenantId") UUID tenantId);

    @Query("SELECT o FROM PollOption o WHERE o.id = :id AND o.tenantId = :tenantId AND o.deleted = false")
    Optional<PollOption> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);
}
