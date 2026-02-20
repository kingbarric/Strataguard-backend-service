package com.strataguard.core.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "poll_votes", indexes = {
        @Index(name = "idx_poll_vote_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_poll_vote_poll_id", columnList = "poll_id"),
        @Index(name = "idx_poll_vote_voter_id", columnList = "voter_id")
},
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_poll_vote_voter_option", columnNames = {"poll_id", "voter_id", "option_id", "tenant_id"})
        })
@Getter
@Setter
public class PollVote extends BaseEntity {

    @Column(name = "poll_id", nullable = false)
    private UUID pollId;

    @Column(name = "option_id", nullable = false)
    private UUID optionId;

    @Column(name = "voter_id", nullable = false)
    private UUID voterId;

    @Column(name = "proxy_for_id")
    private UUID proxyForId;

    @Column(name = "is_proxy_vote", nullable = false)
    private boolean proxyVote = false;
}
