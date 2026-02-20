package com.strataguard.core.entity;

import com.strataguard.core.enums.PollStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "polls", indexes = {
        @Index(name = "idx_poll_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_poll_estate_id", columnList = "estate_id"),
        @Index(name = "idx_poll_status", columnList = "status")
})
@Getter
@Setter
public class Poll extends BaseEntity {

    @Column(name = "estate_id", nullable = false)
    private UUID estateId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PollStatus status = PollStatus.DRAFT;

    @Column(name = "created_by_user_id", nullable = false)
    private String createdByUserId;

    @Column(name = "created_by_name")
    private String createdByName;

    @Column(name = "starts_at")
    private Instant startsAt;

    @Column(name = "deadline", nullable = false)
    private Instant deadline;

    @Column(name = "allow_multiple_choices", nullable = false)
    private boolean allowMultipleChoices = false;

    @Column(name = "is_anonymous", nullable = false)
    private boolean anonymous = false;

    @Column(name = "allow_proxy_voting", nullable = false)
    private boolean allowProxyVoting = false;

    @Column(name = "total_votes", nullable = false)
    private int totalVotes = 0;

    @Column(name = "eligible_voter_count")
    private Integer eligibleVoterCount;
}