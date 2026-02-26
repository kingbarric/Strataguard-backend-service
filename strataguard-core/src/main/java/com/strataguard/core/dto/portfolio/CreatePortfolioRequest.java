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
public class CreatePortfolioRequest {

    @NotBlank(message = "Portfolio name is required")
    private String name;

    private String description;

    private String logoUrl;

    private String contactEmail;

    private String contactPhone;
}
