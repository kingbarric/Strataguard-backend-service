package com.strataguard.core.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "portfolios", indexes = {
    @Index(name = "idx_portfolios_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_portfolios_name", columnList = "name")
})
@Getter
@Setter
@NoArgsConstructor
public class Portfolio extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Column(nullable = false)
    private boolean active = true;
}
