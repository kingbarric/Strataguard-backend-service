package com.strataguard.core.dto.portfolio;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioResponse {

    private UUID id;

    private String name;

    private String description;

    private String logoUrl;

    private String contactEmail;

    private String contactPhone;

    private boolean active;

    private int estateCount;

    private Instant createdAt;

    private Instant updatedAt;
}
