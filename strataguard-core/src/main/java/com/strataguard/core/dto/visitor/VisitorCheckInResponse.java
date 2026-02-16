package com.strataguard.core.dto.visitor;

import com.strataguard.core.enums.VisitPassType;
import com.strataguard.core.enums.VisitorType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class VisitorCheckInResponse {

    private UUID visitorId;
    private String visitorName;
    private String visitorPhone;
    private VisitorType visitorType;
    private String vehiclePlateNumber;
    private String purpose;
    private UUID hostResidentId;
    private String hostName;
    private VisitPassType passType;
    private int usedEntries;
    private Integer maxEntries;
    private Instant validTo;
    private Instant checkInTime;
}
