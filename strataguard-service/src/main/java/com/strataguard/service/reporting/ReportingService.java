package com.strataguard.service.reporting;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.reporting.*;
import com.strataguard.core.entity.Estate;
import com.strataguard.core.enums.*;
import com.strataguard.infrastructure.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReportingService {

    private final EstateRepository estateRepository;
    private final UnitRepository unitRepository;
    private final ResidentRepository residentRepository;
    private final LevyInvoiceRepository levyInvoiceRepository;
    private final PaymentRepository paymentRepository;
    private final VisitorRepository visitorRepository;
    private final GateSessionRepository gateSessionRepository;
    private final SecurityIncidentRepository securityIncidentRepository;
    private final EmergencyAlertRepository emergencyAlertRepository;
    private final PatrolSessionRepository patrolSessionRepository;
    private final CctvCameraRepository cctvCameraRepository;
    private final MaintenanceRequestRepository maintenanceRequestRepository;
    private final ComplaintRepository complaintRepository;
    private final PollRepository pollRepository;

    public OccupancyReportResponse getOccupancyReport() {
        UUID tenantId = TenantContext.requireTenantId();

        long totalUnits = unitRepository.countByTenantId(tenantId);
        long occupied = unitRepository.countByStatusAndTenantId(UnitStatus.OCCUPIED, tenantId);
        long vacant = unitRepository.countByStatusAndTenantId(UnitStatus.VACANT, tenantId);
        long underMaintenance = unitRepository.countByStatusAndTenantId(UnitStatus.UNDER_MAINTENANCE, tenantId);
        long reserved = unitRepository.countByStatusAndTenantId(UnitStatus.RESERVED, tenantId);

        double occupancyRate = totalUnits > 0 ? (double) occupied / totalUnits * 100 : 0;

        Map<String, Long> unitsByStatus = new LinkedHashMap<>();
        for (UnitStatus status : UnitStatus.values()) {
            long count = unitRepository.countByStatusAndTenantId(status, tenantId);
            unitsByStatus.put(status.name(), count);
        }

        List<OccupancyReportResponse.EstateOccupancy> byEstate = new ArrayList<>();
        List<Estate> estates = estateRepository.findAllListByTenantId(tenantId);
        for (Estate estate : estates) {
            long estateTotal = unitRepository.countByEstateIdAndTenantId(estate.getId(), tenantId);
            long estateOccupied = unitRepository.countByEstateIdAndStatusAndTenantId(estate.getId(), UnitStatus.OCCUPIED, tenantId);
            double estateRate = estateTotal > 0 ? (double) estateOccupied / estateTotal * 100 : 0;

            byEstate.add(OccupancyReportResponse.EstateOccupancy.builder()
                    .estateId(estate.getId().toString())
                    .estateName(estate.getName())
                    .totalUnits(estateTotal)
                    .occupiedUnits(estateOccupied)
                    .occupancyRate(Math.round(estateRate * 100.0) / 100.0)
                    .build());
        }

        return OccupancyReportResponse.builder()
                .totalUnits(totalUnits)
                .occupiedUnits(occupied)
                .vacantUnits(vacant)
                .underMaintenanceUnits(underMaintenance)
                .reservedUnits(reserved)
                .occupancyRate(Math.round(occupancyRate * 100.0) / 100.0)
                .unitsByStatus(unitsByStatus)
                .byEstate(byEstate)
                .build();
    }

    public RevenueReportResponse getRevenueReport() {
        UUID tenantId = TenantContext.requireTenantId();

        long totalInvoices = levyInvoiceRepository.countByTenantId(tenantId);
        long overdueInvoices = levyInvoiceRepository.countOverdueByTenantId(tenantId);
        BigDecimal totalBilled = levyInvoiceRepository.sumTotalAmountByTenantId(tenantId);
        BigDecimal totalCollected = levyInvoiceRepository.sumPaidAmountByTenantId(tenantId);
        BigDecimal totalPending = levyInvoiceRepository.sumPendingAmountByTenantId(tenantId);
        BigDecimal totalOverdue = levyInvoiceRepository.sumOverdueAmountByTenantId(tenantId);
        long totalPayments = paymentRepository.countByTenantId(tenantId);

        double collectionRate = totalBilled.compareTo(BigDecimal.ZERO) > 0
                ? totalCollected.divide(totalBilled, 4, RoundingMode.HALF_UP).doubleValue() * 100
                : 0;

        return RevenueReportResponse.builder()
                .totalInvoices(totalInvoices)
                .overdueInvoices(overdueInvoices)
                .totalBilled(totalBilled)
                .totalCollected(totalCollected)
                .totalPending(totalPending)
                .totalOverdue(totalOverdue)
                .collectionRate(Math.round(collectionRate * 100.0) / 100.0)
                .totalPayments(totalPayments)
                .build();
    }

    public VisitorTrafficReportResponse getVisitorTrafficReport() {
        UUID tenantId = TenantContext.requireTenantId();

        long totalVisitors = visitorRepository.countByTenantId(tenantId);
        long checkedIn = visitorRepository.countByStatusAndTenantId(VisitorStatus.CHECKED_IN, tenantId);
        long checkedOut = visitorRepository.countByStatusAndTenantId(VisitorStatus.CHECKED_OUT, tenantId);
        long pending = visitorRepository.countByStatusAndTenantId(VisitorStatus.PENDING, tenantId);
        long expired = visitorRepository.countByStatusAndTenantId(VisitorStatus.EXPIRED, tenantId);
        long denied = visitorRepository.countByStatusAndTenantId(VisitorStatus.DENIED, tenantId);

        long totalGateSessions = gateSessionRepository.countByTenantId(tenantId);
        long openGateSessions = gateSessionRepository.countByStatusAndTenantId(GateSessionStatus.OPEN, tenantId);
        long closedGateSessions = gateSessionRepository.countByStatusAndTenantId(GateSessionStatus.CLOSED, tenantId);

        return VisitorTrafficReportResponse.builder()
                .totalVisitors(totalVisitors)
                .checkedIn(checkedIn)
                .checkedOut(checkedOut)
                .pending(pending)
                .expired(expired)
                .denied(denied)
                .totalGateSessions(totalGateSessions)
                .openGateSessions(openGateSessions)
                .closedGateSessions(closedGateSessions)
                .build();
    }

    public SecurityReportResponse getSecurityReport() {
        UUID tenantId = TenantContext.requireTenantId();

        long totalIncidents = securityIncidentRepository.countByTenantId(tenantId);
        long openIncidents = securityIncidentRepository.countOpenByTenantId(tenantId);
        long resolvedIncidents = securityIncidentRepository.countByStatusAndTenantId(IncidentStatus.RESOLVED, tenantId);

        Map<String, Long> incidentsByCategory = new LinkedHashMap<>();
        for (IncidentCategory cat : IncidentCategory.values()) {
            long count = securityIncidentRepository.countByCategoryAndTenantId(cat, tenantId);
            if (count > 0) {
                incidentsByCategory.put(cat.name(), count);
            }
        }

        Map<String, Long> incidentsBySeverity = new LinkedHashMap<>();
        for (IncidentSeverity sev : IncidentSeverity.values()) {
            long count = securityIncidentRepository.countBySeverityAndTenantId(sev, tenantId);
            if (count > 0) {
                incidentsBySeverity.put(sev.name(), count);
            }
        }

        long totalEmergencyAlerts = emergencyAlertRepository.countByTenantId(tenantId);
        long activeAlerts = emergencyAlertRepository.countActiveByTenantId(tenantId);
        long totalPatrolSessions = patrolSessionRepository.countByTenantId(tenantId);
        long completedPatrols = patrolSessionRepository.countByStatusAndTenantId(PatrolSessionStatus.COMPLETED, tenantId);
        long camerasOnline = cctvCameraRepository.countByStatusAndTenantId(CameraStatus.ONLINE, tenantId);
        long camerasOffline = cctvCameraRepository.countByStatusAndTenantId(CameraStatus.OFFLINE, tenantId);

        return SecurityReportResponse.builder()
                .totalIncidents(totalIncidents)
                .openIncidents(openIncidents)
                .resolvedIncidents(resolvedIncidents)
                .incidentsByCategory(incidentsByCategory)
                .incidentsBySeverity(incidentsBySeverity)
                .totalEmergencyAlerts(totalEmergencyAlerts)
                .activeAlerts(activeAlerts)
                .totalPatrolSessions(totalPatrolSessions)
                .completedPatrols(completedPatrols)
                .camerasOnline(camerasOnline)
                .camerasOffline(camerasOffline)
                .build();
    }

    public DashboardSummaryResponse getDashboardSummary() {
        UUID tenantId = TenantContext.requireTenantId();

        // Property
        long totalEstates = estateRepository.countByTenantId(tenantId);
        long totalUnits = unitRepository.countByTenantId(tenantId);
        long occupiedUnits = unitRepository.countByStatusAndTenantId(UnitStatus.OCCUPIED, tenantId);
        double occupancyRate = totalUnits > 0 ? (double) occupiedUnits / totalUnits * 100 : 0;
        long totalResidents = residentRepository.countByTenantId(tenantId);

        // Financial
        BigDecimal totalRevenue = levyInvoiceRepository.sumPaidAmountByTenantId(tenantId);
        BigDecimal pendingAmount = levyInvoiceRepository.sumPendingAmountByTenantId(tenantId);
        BigDecimal overdueAmount = levyInvoiceRepository.sumOverdueAmountByTenantId(tenantId);
        BigDecimal outstandingAmount = pendingAmount.add(overdueAmount);
        BigDecimal totalBilled = levyInvoiceRepository.sumTotalAmountByTenantId(tenantId);
        double collectionRate = totalBilled.compareTo(BigDecimal.ZERO) > 0
                ? totalRevenue.divide(totalBilled, 4, RoundingMode.HALF_UP).doubleValue() * 100
                : 0;

        // Security
        long openIncidents = securityIncidentRepository.countOpenByTenantId(tenantId);
        long activeAlerts = emergencyAlertRepository.countActiveByTenantId(tenantId);

        // Operations
        long openMaintenance = maintenanceRequestRepository.countByStatusAndTenantId(MaintenanceStatus.OPEN, tenantId);
        long pendingComplaints = complaintRepository.countByStatusAndTenantId(ComplaintStatus.OPEN, tenantId);
        long activePolls = pollRepository.countByStatusAndTenantId(PollStatus.ACTIVE, tenantId);

        // Visitors
        long todayVisitors = visitorRepository.countByStatusAndTenantId(VisitorStatus.CHECKED_IN, tenantId);
        long openGateSessions = gateSessionRepository.countByStatusAndTenantId(GateSessionStatus.OPEN, tenantId);

        return DashboardSummaryResponse.builder()
                .totalEstates(totalEstates)
                .totalUnits(totalUnits)
                .occupancyRate(Math.round(occupancyRate * 100.0) / 100.0)
                .totalResidents(totalResidents)
                .totalRevenue(totalRevenue)
                .outstandingAmount(outstandingAmount)
                .collectionRate(Math.round(collectionRate * 100.0) / 100.0)
                .openIncidents(openIncidents)
                .activeAlerts(activeAlerts)
                .openMaintenanceRequests(openMaintenance)
                .pendingComplaints(pendingComplaints)
                .activePolls(activePolls)
                .todayVisitors(todayVisitors)
                .openGateSessions(openGateSessions)
                .build();
    }
}
