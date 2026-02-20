package com.strataguard.service.reporting;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.reporting.*;
import com.strataguard.core.entity.Estate;
import com.strataguard.core.enums.*;
import com.strataguard.infrastructure.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportingServiceTest {

    @Mock private EstateRepository estateRepository;
    @Mock private UnitRepository unitRepository;
    @Mock private ResidentRepository residentRepository;
    @Mock private LevyInvoiceRepository levyInvoiceRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private VisitorRepository visitorRepository;
    @Mock private GateSessionRepository gateSessionRepository;
    @Mock private SecurityIncidentRepository securityIncidentRepository;
    @Mock private EmergencyAlertRepository emergencyAlertRepository;
    @Mock private PatrolSessionRepository patrolSessionRepository;
    @Mock private CctvCameraRepository cctvCameraRepository;
    @Mock private MaintenanceRequestRepository maintenanceRequestRepository;
    @Mock private ComplaintRepository complaintRepository;
    @Mock private PollRepository pollRepository;

    @InjectMocks
    private ReportingService reportingService;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ===== Occupancy Report =====

    @Test
    void getOccupancyReport_shouldReturnCorrectCounts() {
        when(unitRepository.countByTenantId(tenantId)).thenReturn(100L);
        when(unitRepository.countByStatusAndTenantId(UnitStatus.OCCUPIED, tenantId)).thenReturn(75L);
        when(unitRepository.countByStatusAndTenantId(UnitStatus.VACANT, tenantId)).thenReturn(20L);
        when(unitRepository.countByStatusAndTenantId(UnitStatus.UNDER_MAINTENANCE, tenantId)).thenReturn(3L);
        when(unitRepository.countByStatusAndTenantId(UnitStatus.RESERVED, tenantId)).thenReturn(2L);
        when(estateRepository.findAllListByTenantId(tenantId)).thenReturn(List.of());

        OccupancyReportResponse result = reportingService.getOccupancyReport();

        assertThat(result.getTotalUnits()).isEqualTo(100);
        assertThat(result.getOccupiedUnits()).isEqualTo(75);
        assertThat(result.getVacantUnits()).isEqualTo(20);
        assertThat(result.getUnderMaintenanceUnits()).isEqualTo(3);
        assertThat(result.getReservedUnits()).isEqualTo(2);
        assertThat(result.getOccupancyRate()).isEqualTo(75.0);
    }

    @Test
    void getOccupancyReport_withEstates_shouldIncludeEstateBreakdown() {
        Estate estate = new Estate();
        estate.setId(UUID.randomUUID());
        estate.setName("Sunrise Estate");

        when(unitRepository.countByTenantId(tenantId)).thenReturn(50L);
        when(unitRepository.countByStatusAndTenantId(any(UnitStatus.class), eq(tenantId))).thenReturn(0L);
        when(unitRepository.countByStatusAndTenantId(UnitStatus.OCCUPIED, tenantId)).thenReturn(30L);
        when(estateRepository.findAllListByTenantId(tenantId)).thenReturn(List.of(estate));
        when(unitRepository.countByEstateIdAndTenantId(estate.getId(), tenantId)).thenReturn(50L);
        when(unitRepository.countByEstateIdAndStatusAndTenantId(estate.getId(), UnitStatus.OCCUPIED, tenantId)).thenReturn(30L);

        OccupancyReportResponse result = reportingService.getOccupancyReport();

        assertThat(result.getByEstate()).hasSize(1);
        assertThat(result.getByEstate().get(0).getEstateName()).isEqualTo("Sunrise Estate");
        assertThat(result.getByEstate().get(0).getOccupiedUnits()).isEqualTo(30);
        assertThat(result.getByEstate().get(0).getOccupancyRate()).isEqualTo(60.0);
    }

    @Test
    void getOccupancyReport_noUnits_shouldReturnZeroRate() {
        when(unitRepository.countByTenantId(tenantId)).thenReturn(0L);
        when(unitRepository.countByStatusAndTenantId(any(UnitStatus.class), eq(tenantId))).thenReturn(0L);
        when(estateRepository.findAllListByTenantId(tenantId)).thenReturn(List.of());

        OccupancyReportResponse result = reportingService.getOccupancyReport();

        assertThat(result.getOccupancyRate()).isEqualTo(0.0);
    }

    // ===== Revenue Report =====

    @Test
    void getRevenueReport_shouldReturnFinancialSummary() {
        when(levyInvoiceRepository.countByTenantId(tenantId)).thenReturn(200L);
        when(levyInvoiceRepository.countOverdueByTenantId(tenantId)).thenReturn(15L);
        when(levyInvoiceRepository.sumTotalAmountByTenantId(tenantId)).thenReturn(new BigDecimal("500000.00"));
        when(levyInvoiceRepository.sumPaidAmountByTenantId(tenantId)).thenReturn(new BigDecimal("400000.00"));
        when(levyInvoiceRepository.sumPendingAmountByTenantId(tenantId)).thenReturn(new BigDecimal("75000.00"));
        when(levyInvoiceRepository.sumOverdueAmountByTenantId(tenantId)).thenReturn(new BigDecimal("25000.00"));
        when(paymentRepository.countByTenantId(tenantId)).thenReturn(180L);

        RevenueReportResponse result = reportingService.getRevenueReport();

        assertThat(result.getTotalInvoices()).isEqualTo(200);
        assertThat(result.getOverdueInvoices()).isEqualTo(15);
        assertThat(result.getTotalBilled()).isEqualByComparingTo(new BigDecimal("500000.00"));
        assertThat(result.getTotalCollected()).isEqualByComparingTo(new BigDecimal("400000.00"));
        assertThat(result.getCollectionRate()).isEqualTo(80.0);
        assertThat(result.getTotalPayments()).isEqualTo(180);
    }

    @Test
    void getRevenueReport_zeroBilled_shouldReturnZeroCollectionRate() {
        when(levyInvoiceRepository.countByTenantId(tenantId)).thenReturn(0L);
        when(levyInvoiceRepository.countOverdueByTenantId(tenantId)).thenReturn(0L);
        when(levyInvoiceRepository.sumTotalAmountByTenantId(tenantId)).thenReturn(BigDecimal.ZERO);
        when(levyInvoiceRepository.sumPaidAmountByTenantId(tenantId)).thenReturn(BigDecimal.ZERO);
        when(levyInvoiceRepository.sumPendingAmountByTenantId(tenantId)).thenReturn(BigDecimal.ZERO);
        when(levyInvoiceRepository.sumOverdueAmountByTenantId(tenantId)).thenReturn(BigDecimal.ZERO);
        when(paymentRepository.countByTenantId(tenantId)).thenReturn(0L);

        RevenueReportResponse result = reportingService.getRevenueReport();

        assertThat(result.getCollectionRate()).isEqualTo(0.0);
    }

    // ===== Visitor Traffic Report =====

    @Test
    void getVisitorTrafficReport_shouldReturnVisitorAndGateStats() {
        when(visitorRepository.countByTenantId(tenantId)).thenReturn(500L);
        when(visitorRepository.countByStatusAndTenantId(VisitorStatus.CHECKED_IN, tenantId)).thenReturn(50L);
        when(visitorRepository.countByStatusAndTenantId(VisitorStatus.CHECKED_OUT, tenantId)).thenReturn(400L);
        when(visitorRepository.countByStatusAndTenantId(VisitorStatus.PENDING, tenantId)).thenReturn(30L);
        when(visitorRepository.countByStatusAndTenantId(VisitorStatus.EXPIRED, tenantId)).thenReturn(15L);
        when(visitorRepository.countByStatusAndTenantId(VisitorStatus.DENIED, tenantId)).thenReturn(5L);
        when(gateSessionRepository.countByTenantId(tenantId)).thenReturn(450L);
        when(gateSessionRepository.countByStatusAndTenantId(GateSessionStatus.OPEN, tenantId)).thenReturn(10L);
        when(gateSessionRepository.countByStatusAndTenantId(GateSessionStatus.CLOSED, tenantId)).thenReturn(440L);

        VisitorTrafficReportResponse result = reportingService.getVisitorTrafficReport();

        assertThat(result.getTotalVisitors()).isEqualTo(500);
        assertThat(result.getCheckedIn()).isEqualTo(50);
        assertThat(result.getCheckedOut()).isEqualTo(400);
        assertThat(result.getPending()).isEqualTo(30);
        assertThat(result.getExpired()).isEqualTo(15);
        assertThat(result.getDenied()).isEqualTo(5);
        assertThat(result.getTotalGateSessions()).isEqualTo(450);
        assertThat(result.getOpenGateSessions()).isEqualTo(10);
        assertThat(result.getClosedGateSessions()).isEqualTo(440);
    }

    // ===== Security Report =====

    @Test
    void getSecurityReport_shouldReturnSecurityMetrics() {
        when(securityIncidentRepository.countByTenantId(tenantId)).thenReturn(30L);
        when(securityIncidentRepository.countOpenByTenantId(tenantId)).thenReturn(5L);
        when(securityIncidentRepository.countByStatusAndTenantId(IncidentStatus.RESOLVED, tenantId)).thenReturn(20L);
        when(securityIncidentRepository.countByCategoryAndTenantId(any(IncidentCategory.class), eq(tenantId))).thenReturn(0L);
        when(securityIncidentRepository.countByCategoryAndTenantId(IncidentCategory.THEFT, tenantId)).thenReturn(8L);
        when(securityIncidentRepository.countByCategoryAndTenantId(IncidentCategory.TRESPASSING, tenantId)).thenReturn(5L);
        when(securityIncidentRepository.countBySeverityAndTenantId(any(IncidentSeverity.class), eq(tenantId))).thenReturn(0L);
        when(securityIncidentRepository.countBySeverityAndTenantId(IncidentSeverity.HIGH, tenantId)).thenReturn(3L);
        when(securityIncidentRepository.countBySeverityAndTenantId(IncidentSeverity.LOW, tenantId)).thenReturn(10L);
        when(emergencyAlertRepository.countByTenantId(tenantId)).thenReturn(12L);
        when(emergencyAlertRepository.countActiveByTenantId(tenantId)).thenReturn(2L);
        when(patrolSessionRepository.countByTenantId(tenantId)).thenReturn(100L);
        when(patrolSessionRepository.countByStatusAndTenantId(PatrolSessionStatus.COMPLETED, tenantId)).thenReturn(90L);
        when(cctvCameraRepository.countByStatusAndTenantId(CameraStatus.ONLINE, tenantId)).thenReturn(20L);
        when(cctvCameraRepository.countByStatusAndTenantId(CameraStatus.OFFLINE, tenantId)).thenReturn(2L);

        SecurityReportResponse result = reportingService.getSecurityReport();

        assertThat(result.getTotalIncidents()).isEqualTo(30);
        assertThat(result.getOpenIncidents()).isEqualTo(5);
        assertThat(result.getResolvedIncidents()).isEqualTo(20);
        assertThat(result.getIncidentsByCategory()).containsEntry("THEFT", 8L);
        assertThat(result.getIncidentsByCategory()).containsEntry("TRESPASSING", 5L);
        assertThat(result.getIncidentsBySeverity()).containsEntry("HIGH", 3L);
        assertThat(result.getTotalEmergencyAlerts()).isEqualTo(12);
        assertThat(result.getActiveAlerts()).isEqualTo(2);
        assertThat(result.getTotalPatrolSessions()).isEqualTo(100);
        assertThat(result.getCompletedPatrols()).isEqualTo(90);
        assertThat(result.getCamerasOnline()).isEqualTo(20);
        assertThat(result.getCamerasOffline()).isEqualTo(2);
    }

    // ===== Dashboard Summary =====

    @Test
    void getDashboardSummary_shouldReturnCombinedMetrics() {
        // Property
        when(estateRepository.countByTenantId(tenantId)).thenReturn(5L);
        when(unitRepository.countByTenantId(tenantId)).thenReturn(100L);
        when(unitRepository.countByStatusAndTenantId(UnitStatus.OCCUPIED, tenantId)).thenReturn(80L);
        when(residentRepository.countByTenantId(tenantId)).thenReturn(150L);

        // Financial
        when(levyInvoiceRepository.sumPaidAmountByTenantId(tenantId)).thenReturn(new BigDecimal("300000.00"));
        when(levyInvoiceRepository.sumPendingAmountByTenantId(tenantId)).thenReturn(new BigDecimal("50000.00"));
        when(levyInvoiceRepository.sumOverdueAmountByTenantId(tenantId)).thenReturn(new BigDecimal("20000.00"));
        when(levyInvoiceRepository.sumTotalAmountByTenantId(tenantId)).thenReturn(new BigDecimal("370000.00"));

        // Security
        when(securityIncidentRepository.countOpenByTenantId(tenantId)).thenReturn(3L);
        when(emergencyAlertRepository.countActiveByTenantId(tenantId)).thenReturn(1L);

        // Operations
        when(maintenanceRequestRepository.countByStatusAndTenantId(MaintenanceStatus.OPEN, tenantId)).thenReturn(8L);
        when(complaintRepository.countByStatusAndTenantId(ComplaintStatus.OPEN, tenantId)).thenReturn(4L);
        when(pollRepository.countByStatusAndTenantId(PollStatus.ACTIVE, tenantId)).thenReturn(2L);

        // Visitors
        when(visitorRepository.countByStatusAndTenantId(VisitorStatus.CHECKED_IN, tenantId)).thenReturn(12L);
        when(gateSessionRepository.countByStatusAndTenantId(GateSessionStatus.OPEN, tenantId)).thenReturn(3L);

        DashboardSummaryResponse result = reportingService.getDashboardSummary();

        assertThat(result.getTotalEstates()).isEqualTo(5);
        assertThat(result.getTotalUnits()).isEqualTo(100);
        assertThat(result.getOccupancyRate()).isEqualTo(80.0);
        assertThat(result.getTotalResidents()).isEqualTo(150);
        assertThat(result.getTotalRevenue()).isEqualByComparingTo(new BigDecimal("300000.00"));
        assertThat(result.getOutstandingAmount()).isEqualByComparingTo(new BigDecimal("70000.00"));
        assertThat(result.getOpenIncidents()).isEqualTo(3);
        assertThat(result.getActiveAlerts()).isEqualTo(1);
        assertThat(result.getOpenMaintenanceRequests()).isEqualTo(8);
        assertThat(result.getPendingComplaints()).isEqualTo(4);
        assertThat(result.getActivePolls()).isEqualTo(2);
        assertThat(result.getTodayVisitors()).isEqualTo(12);
        assertThat(result.getOpenGateSessions()).isEqualTo(3);
    }

    @Test
    void getDashboardSummary_emptyData_shouldReturnZeros() {
        when(estateRepository.countByTenantId(tenantId)).thenReturn(0L);
        when(unitRepository.countByTenantId(tenantId)).thenReturn(0L);
        when(unitRepository.countByStatusAndTenantId(UnitStatus.OCCUPIED, tenantId)).thenReturn(0L);
        when(residentRepository.countByTenantId(tenantId)).thenReturn(0L);
        when(levyInvoiceRepository.sumPaidAmountByTenantId(tenantId)).thenReturn(BigDecimal.ZERO);
        when(levyInvoiceRepository.sumPendingAmountByTenantId(tenantId)).thenReturn(BigDecimal.ZERO);
        when(levyInvoiceRepository.sumOverdueAmountByTenantId(tenantId)).thenReturn(BigDecimal.ZERO);
        when(levyInvoiceRepository.sumTotalAmountByTenantId(tenantId)).thenReturn(BigDecimal.ZERO);
        when(securityIncidentRepository.countOpenByTenantId(tenantId)).thenReturn(0L);
        when(emergencyAlertRepository.countActiveByTenantId(tenantId)).thenReturn(0L);
        when(maintenanceRequestRepository.countByStatusAndTenantId(MaintenanceStatus.OPEN, tenantId)).thenReturn(0L);
        when(complaintRepository.countByStatusAndTenantId(ComplaintStatus.OPEN, tenantId)).thenReturn(0L);
        when(pollRepository.countByStatusAndTenantId(PollStatus.ACTIVE, tenantId)).thenReturn(0L);
        when(visitorRepository.countByStatusAndTenantId(VisitorStatus.CHECKED_IN, tenantId)).thenReturn(0L);
        when(gateSessionRepository.countByStatusAndTenantId(GateSessionStatus.OPEN, tenantId)).thenReturn(0L);

        DashboardSummaryResponse result = reportingService.getDashboardSummary();

        assertThat(result.getTotalEstates()).isZero();
        assertThat(result.getOccupancyRate()).isEqualTo(0.0);
        assertThat(result.getCollectionRate()).isEqualTo(0.0);
    }
}
