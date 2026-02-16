package com.strataguard.core.dto.visitor;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdateVisitorRequest {

    private String name;

    private String phone;

    private String email;

    private String purpose;

    private String vehiclePlateNumber;
}
