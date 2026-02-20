package com.strataguard.service.security;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.security.IncidentResponse;
import com.strataguard.core.dto.security.SecurityDashboardResponse;
import com.strataguard.core.enums.CameraStatus;
import com.strataguard.core.enums.IncidentSeverity;
import com.strataguard.core.util.SecurityIncidentMapper;
import com.strataguard.infrastructure.repository.CctvCameraRepository;
import com.strataguard.infrastructure.repository.EmergencyAlertRepository;
import com.strataguard.infrastructure.repository.PatrolSessionRepository;
import com.strataguard.infrastructure.repository.SecurityIncidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SecurityDashboardService {

    private final StaffService staffService;
    private final SecurityIncidentRepository securityIncidentRepository;
    private final SecurityIncidentMapper securityIncidentMapper;
    private final EmergencyAlertRepository emergencyAlertRepository;
    private final CctvCameraRepository cctvCameraRepository;
    private final PatrolSessionRepository patrolSessionRepository;

    public SecurityDashboardResponse getDashboard(UUID estateId) {
        UUID tenantId = TenantContext.requireTenantId();

        // 1. On-duty staff count
        long onDutyStaffCount = staffService.getOnDutyStaff(estateId).size();

        // 2. Open incidents
        long openIncidents = securityIncidentRepository.countOpenByTenantId(tenantId);

        // 3. Critical incidents
        long criticalIncidents = securityIncidentRepository.countOpenBySeverityAndTenantId(IncidentSeverity.CRITICAL, tenantId);

        // 4. High incidents
        long highIncidents = securityIncidentRepository.countOpenBySeverityAndTenantId(IncidentSeverity.HIGH, tenantId);

        // 5. Patrol completion rate (today)
        Instant todayStart = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant todayEnd = todayStart.plus(1, ChronoUnit.DAYS);
        Double avg = patrolSessionRepository.avgCompletionByEstateAndDateRange(estateId, tenantId, todayStart, todayEnd);
        double patrolCompletionRate = avg != null ? avg : 0.0;

        // 6. Average emergency response time (seconds)
        Double avgResponse = emergencyAlertRepository.avgResponseTimeByTenantId(tenantId);
        double avgEmergencyResponseSeconds = avgResponse != null ? avgResponse : 0.0;

        // 7. Active emergencies
        long activeEmergencies = emergencyAlertRepository.countActiveByTenantId(tenantId);

        // 8. Total cameras
        long totalCameras = cctvCameraRepository.countByTenantId(tenantId);

        // 9. Online cameras
        long onlineCameras = cctvCameraRepository.countByStatusAndTenantId(CameraStatus.ONLINE, tenantId);

        // 10. Offline cameras
        long offlineCameras = cctvCameraRepository.countByStatusAndTenantId(CameraStatus.OFFLINE, tenantId);

        // 11. Recent incidents (last 10)
        List<IncidentResponse> recentIncidents = securityIncidentRepository
                .findRecentByTenantId(tenantId, PageRequest.of(0, 10))
                .map(securityIncidentMapper::toResponse)
                .getContent();

        return SecurityDashboardResponse.builder()
                .onDutyStaffCount(onDutyStaffCount)
                .openIncidents(openIncidents)
                .criticalIncidents(criticalIncidents)
                .highIncidents(highIncidents)
                .patrolCompletionRate(patrolCompletionRate)
                .avgEmergencyResponseSeconds(avgEmergencyResponseSeconds)
                .activeEmergencies(activeEmergencies)
                .totalCameras(totalCameras)
                .onlineCameras(onlineCameras)
                .offlineCameras(offlineCameras)
                .recentIncidents(recentIncidents)
                .build();
    }
}
