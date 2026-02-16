package com.strataguard.core.dto.blacklist;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class BlacklistResponse {

    private UUID id;
    private String name;
    private String phone;
    private String plateNumber;
    private String reason;
    private boolean active;
    private String addedBy;
    private Instant createdAt;
    private Instant updatedAt;
}
