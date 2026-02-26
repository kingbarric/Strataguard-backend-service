package com.strataguard.core.entity;

import com.strataguard.core.enums.EstateType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "estates", indexes = {
        @Index(name = "idx_estates_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_estates_name", columnList = "name")
})
@Getter
@Setter
@NoArgsConstructor
public class Estate extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    private String city;

    private String state;

    private String country;

    @Enumerated(EnumType.STRING)
    @Column(name = "estate_type", nullable = false)
    private EstateType estateType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Column(name = "total_units")
    private Integer totalUnits;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "settings", columnDefinition = "jsonb")
    private String settings;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "portfolio_id")
    private UUID portfolioId;
}
