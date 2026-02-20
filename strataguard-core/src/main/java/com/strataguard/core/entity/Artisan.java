package com.strataguard.core.entity;

import com.strataguard.core.enums.ArtisanCategory;
import com.strataguard.core.enums.ArtisanStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "artisans", indexes = {
        @Index(name = "idx_artisan_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_artisan_estate_id", columnList = "estate_id"),
        @Index(name = "idx_artisan_category", columnList = "category"),
        @Index(name = "idx_artisan_status", columnList = "status")
})
@Getter
@Setter
public class Artisan extends BaseEntity {

    @Column(name = "estate_id", nullable = false)
    private UUID estateId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "phone", nullable = false)
    private String phone;

    @Column(name = "email")
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private ArtisanCategory category;

    @Column(name = "specialization")
    private String specialization;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(name = "address")
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ArtisanStatus status = ArtisanStatus.ACTIVE;

    @Column(name = "verified", nullable = false)
    private boolean verified = false;

    @Column(name = "total_jobs", nullable = false)
    private int totalJobs = 0;

    @Column(name = "total_rating", nullable = false)
    private double totalRating = 0.0;

    @Column(name = "rating_count", nullable = false)
    private int ratingCount = 0;

    @Column(name = "average_rating", nullable = false)
    private double averageRating = 0.0;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}