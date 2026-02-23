package com.strataguard.core.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "charge_reminder_logs", indexes = {
        @Index(name = "idx_charge_reminder_logs_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_charge_reminder_logs_invoice_id", columnList = "invoice_id")
})
@Getter
@Setter
@NoArgsConstructor
public class ChargeReminderLog extends BaseEntity {

    @Column(name = "invoice_id", nullable = false)
    private UUID invoiceId;

    @Column(name = "days_before", nullable = false)
    private int daysBefore;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;
}
