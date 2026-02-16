package com.strataguard.core.entity;

import com.strataguard.core.enums.VisitorStatus;
import com.strataguard.core.enums.VisitorType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "visitors", indexes = {
        @Index(name = "idx_visitors_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_visitors_invited_by", columnList = "invited_by"),
        @Index(name = "idx_visitors_status", columnList = "status"),
        @Index(name = "idx_visitors_phone", columnList = "phone")
})
@Getter
@Setter
@NoArgsConstructor
public class Visitor extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "phone")
    private String phone;

    @Column(name = "email")
    private String email;

    @Column(name = "purpose")
    private String purpose;

    @Column(name = "invited_by", nullable = false)
    private UUID invitedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "visitor_type", nullable = false)
    private VisitorType visitorType;

    @Column(name = "vehicle_plate_number")
    private String vehiclePlateNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private VisitorStatus status = VisitorStatus.PENDING;
}
