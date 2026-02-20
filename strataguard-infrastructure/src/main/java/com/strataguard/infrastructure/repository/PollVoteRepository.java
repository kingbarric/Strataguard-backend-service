package com.strataguard.infrastructure.repository;

import com.strataguard.core.entity.PollVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PollVoteRepository extends JpaRepository<PollVote, UUID> {

    @Query("SELECT v FROM PollVote v WHERE v.pollId = :pollId AND v.voterId = :voterId AND v.tenantId = :tenantId AND v.deleted = false")
    List<PollVote> findByPollIdAndVoterId(@Param("pollId") UUID pollId, @Param("voterId") UUID voterId, @Param("tenantId") UUID tenantId);

    @Query("SELECT CASE WHEN COUNT(v) > 0 THEN true ELSE false END FROM PollVote v " +
            "WHERE v.pollId = :pollId AND v.voterId = :voterId AND v.tenantId = :tenantId AND v.deleted = false")
    boolean existsByPollIdAndVoterId(@Param("pollId") UUID pollId, @Param("voterId") UUID voterId, @Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(DISTINCT v.voterId) FROM PollVote v WHERE v.pollId = :pollId AND v.tenantId = :tenantId AND v.deleted = false")
    long countDistinctVotersByPollId(@Param("pollId") UUID pollId, @Param("tenantId") UUID tenantId);
}
