package com.estatekit.core.dto.visitor;

import lombok.Data;

@Data
public class VisitorCheckInRequest {

    private String token;

    private String verificationCode;

    private String note;
}
