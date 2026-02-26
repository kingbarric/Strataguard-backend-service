package com.strataguard.core.dto.portfolio;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePortfolioRequest {

    private String name;

    private String description;

    private String logoUrl;

    private String contactEmail;

    private String contactPhone;

    private Boolean active;
}
