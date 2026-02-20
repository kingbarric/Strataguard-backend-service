package com.strataguard.core.dto.reporting;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class RevenueReportResponse {
    private long totalInvoices;
    private long overdueInvoices;
    private BigDecimal totalBilled;
    private BigDecimal totalCollected;
    private BigDecimal totalPending;
    private BigDecimal totalOverdue;
    private double collectionRate;
    private long totalPayments;
}
