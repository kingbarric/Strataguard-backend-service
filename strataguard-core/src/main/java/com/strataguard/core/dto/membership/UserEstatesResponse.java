package com.strataguard.core.dto.membership;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEstatesResponse {
    private String userId;
    private List<EstateMembershipResponse> memberships;
}
