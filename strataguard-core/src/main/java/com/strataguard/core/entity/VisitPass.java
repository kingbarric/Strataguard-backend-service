package com.strataguard.core.entity;

import com.strataguard.core.enums.VisitPassStatus;
import com.strataguard.core.enums.VisitPassType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "visit_passes", indexes = {
        @Index(name = "idx_visit_passes_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_visit_passes_visitor_id", columnList = "visitor_id"),
        @Index(name = "idx_visit_passes_pass_code", columnList = "pass_code"),
        @Index(name = "idx_visit_passes_verification_code", columnList = "verification_code")
})
@Getter
@Setter
@NoArgsConstructor
public class VisitPass extends BaseEntity {

    @Column(name = "visitor_id", nullable = false)
    private UUID visitorId;

    @Column(name = "pass_code", nullable = false, unique = true)
    private String passCode;

    @Column(name = "qr_data", columnDefinition = "TEXT")
    private String qrData;

    @Column(name = "token", nullable = false, columnDefinition = "TEXT")
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "pass_type", nullable = false)
    private VisitPassType passType;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_to", nullable = false)
    private Instant validTo;

    @Column(name = "max_entries")
    private Integer maxEntries;

    @Column(name = "used_entries", nullable = false)
    private int usedEntries = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private VisitPassStatus status = VisitPassStatus.ACTIVE;

    @Column(name = "verification_code", nullable = false, length = 6)
    private String verificationCode;

    @Column(name = "recurring_days")
    private String recurringDays;

    @Column(name = "recurring_start_time")
    private LocalTime recurringStartTime;

    @Column(name = "recurring_end_time")
    private LocalTime recurringEndTime;
}
