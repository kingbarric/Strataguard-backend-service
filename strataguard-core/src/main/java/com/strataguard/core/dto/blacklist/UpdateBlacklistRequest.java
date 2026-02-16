package com.strataguard.core.dto.blacklist;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdateBlacklistRequest {

    private String name;

    private String phone;

    private String plateNumber;

    private String reason;

    private Boolean active;
}
