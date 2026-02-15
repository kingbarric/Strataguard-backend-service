package com.estatekit.core.entity;

import com.estatekit.core.enums.ExitApprovalStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "exit_approval_requests", indexes = {
        @Index(name = "idx_exit_approval_requests_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_exit_approval_requests_session_id", columnList = "session_id"),
        @Index(name = "idx_exit_approval_requests_resident_id", columnList = "resident_id"),
        @Index(name = "idx_exit_approval_requests_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
public class ExitApprovalRequest extends BaseEntity {

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;

    @Column(name = "resident_id", nullable = false)
    private UUID residentId;

    @Column(name = "guard_id", nullable = false)
    private String guardId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExitApprovalStatus status = ExitApprovalStatus.PENDING;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "responded_at")
    private Instant respondedAt;

    @Column(name = "note")
    private String note;
}
