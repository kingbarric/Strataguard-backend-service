package com.estatekit.core.dto.visitor;

import com.estatekit.core.enums.VisitPassStatus;
import com.estatekit.core.enums.VisitPassType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
public class VisitPassResponse {

    private UUID id;
    private UUID visitorId;
    private String passCode;
    private String qrData;
    private String token;
    private String verificationCode;
    private VisitPassType passType;
    private Instant validFrom;
    private Instant validTo;
    private Integer maxEntries;
    private int usedEntries;
    private VisitPassStatus status;
    private String recurringDays;
    private LocalTime recurringStartTime;
    private LocalTime recurringEndTime;
    private Instant createdAt;
}
