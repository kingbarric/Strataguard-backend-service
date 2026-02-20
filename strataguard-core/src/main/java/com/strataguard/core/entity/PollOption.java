package com.strataguard.core.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "poll_options", indexes = {
        @Index(name = "idx_poll_option_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_poll_option_poll_id", columnList = "poll_id")
})
@Getter
@Setter
public class PollOption extends BaseEntity {

    @Column(name = "poll_id", nullable = false)
    private UUID pollId;

    @Column(name = "option_text", nullable = false)
    private String optionText;

    @Column(name = "vote_count", nullable = false)
    private int voteCount = 0;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;
}