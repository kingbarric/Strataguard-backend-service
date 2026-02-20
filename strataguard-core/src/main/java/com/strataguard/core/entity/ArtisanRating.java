package com.strataguard.core.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "artisan_ratings", indexes = {
        @Index(name = "idx_artisan_rating_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_artisan_rating_artisan_id", columnList = "artisan_id"),
        @Index(name = "idx_artisan_rating_resident_id", columnList = "resident_id")
})
@Getter
@Setter
public class ArtisanRating extends BaseEntity {

    @Column(name = "artisan_id", nullable = false)
    private UUID artisanId;

    @Column(name = "resident_id", nullable = false)
    private UUID residentId;

    @Column(name = "maintenance_request_id")
    private UUID maintenanceRequestId;

    @Column(name = "rating", nullable = false)
    private int rating;

    @Column(name = "review", columnDefinition = "TEXT")
    private String review;
}