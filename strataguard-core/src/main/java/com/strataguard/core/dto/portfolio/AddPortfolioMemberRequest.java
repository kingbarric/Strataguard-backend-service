package com.strataguard.core.dto.portfolio;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddPortfolioMemberRequest {

    @NotBlank(message = "User ID is required")
    private String userId;

    @NotBlank(message = "Role is required (PORTFOLIO_ADMIN or PORTFOLIO_VIEWER)")
    private String role;
}
