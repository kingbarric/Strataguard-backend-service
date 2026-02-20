package com.strataguard.core.dto.governance;

import com.strataguard.core.enums.ArtisanCategory;
import com.strataguard.core.enums.ArtisanStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class ArtisanResponse {
    private UUID id;
    private UUID estateId;
    private String name;
    private String phone;
    private String email;
    private ArtisanCategory category;
    private String specialization;
    private String description;
    private String photoUrl;
    private String address;
    private ArtisanStatus status;
    private boolean verified;
    private int totalJobs;
    private double averageRating;
    private int ratingCount;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
}
