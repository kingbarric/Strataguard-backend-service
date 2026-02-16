package com.strataguard.core.dto.visitor;

import com.strataguard.core.enums.VisitorStatus;
import com.strataguard.core.enums.VisitorType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class VisitorResponse {

    private UUID id;
    private String name;
    private String phone;
    private String email;
    private String purpose;
    private UUID invitedBy;
    private String invitedByName;
    private VisitorType visitorType;
    private String vehiclePlateNumber;
    private VisitorStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}
