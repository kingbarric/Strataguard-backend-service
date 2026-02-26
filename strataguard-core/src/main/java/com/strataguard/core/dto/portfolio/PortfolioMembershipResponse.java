package com.strataguard.core.dto.portfolio;

import com.strataguard.core.enums.MembershipStatus;
import com.strataguard.core.enums.UserRole;
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
public class PortfolioMembershipResponse {

    private UUID id;

    private String userId;

    private UUID portfolioId;

    private String portfolioName;

    private UserRole role;

    private MembershipStatus status;

    private Instant createdAt;
}
