package com.strataguard.service.security;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.dto.security.*;
import com.strataguard.core.entity.Staff;
import com.strataguard.core.entity.StaffShift;
import com.strataguard.core.enums.StaffDepartment;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.core.util.StaffMapper;
import com.strataguard.core.util.StaffShiftMapper;
import com.strataguard.infrastructure.repository.StaffRepository;
import com.strataguard.infrastructure.repository.StaffShiftRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StaffService {

    private final StaffRepository staffRepository;
    private final StaffShiftRepository staffShiftRepository;
    private final StaffMapper staffMapper;
    private final StaffShiftMapper staffShiftMapper;

    public StaffResponse createStaff(CreateStaffRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        // Check badge uniqueness if provided
        if (request.getBadgeNumber() != null && !request.getBadgeNumber().isBlank()) {
            if (staffRepository.existsByBadgeNumberAndTenantId(request.getBadgeNumber(), tenantId)) {
                throw new IllegalArgumentException("Badge number already in use");
            }
        }

        Staff staff = staffMapper.toEntity(request);
        staff.setTenantId(tenantId);

        Staff saved = staffRepository.save(staff);
        log.info("Created staff: {} for tenant: {}", saved.getId(), tenantId);
        return staffMapper.toResponse(saved);
    }

    public StaffResponse updateStaff(UUID id, UpdateStaffRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        Staff staff = staffRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff", "id", id));

        // Check badge uniqueness if changed
        if (request.getBadgeNumber() != null && !request.getBadgeNumber().isBlank()) {
            boolean badgeChanged = !request.getBadgeNumber().equals(staff.getBadgeNumber());
            if (badgeChanged && staffRepository.existsByBadgeNumberAndTenantId(request.getBadgeNumber(), tenantId)) {
                throw new IllegalArgumentException("Badge number already in use");
            }
        }

        staffMapper.updateEntity(request, staff);
        Staff updated = staffRepository.save(staff);
        log.info("Updated staff: {} for tenant: {}", id, tenantId);
        return staffMapper.toResponse(updated);
    }

    @Transactional(readOnly = true)
    public StaffResponse getStaff(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();

        Staff staff = staffRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff", "id", id));

        return staffMapper.toResponse(staff);
    }

    @Transactional(readOnly = true)
    public PagedResponse<StaffResponse> getAllStaff(Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<Staff> page = staffRepository.findAllByTenantId(tenantId, pageable);

        List<StaffResponse> content = page.getContent().stream()
                .map(staffMapper::toResponse)
                .toList();

        return toPagedResponse(page, content);
    }

    @Transactional(readOnly = true)
    public PagedResponse<StaffResponse> getStaffByEstate(UUID estateId, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<Staff> page = staffRepository.findByEstateIdAndTenantId(estateId, tenantId, pageable);

        List<StaffResponse> content = page.getContent().stream()
                .map(staffMapper::toResponse)
                .toList();

        return toPagedResponse(page, content);
    }

    @Transactional(readOnly = true)
    public PagedResponse<StaffResponse> getStaffByDepartment(StaffDepartment department, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<Staff> page = staffRepository.findByDepartmentAndTenantId(department, tenantId, pageable);

        List<StaffResponse> content = page.getContent().stream()
                .map(staffMapper::toResponse)
                .toList();

        return toPagedResponse(page, content);
    }

    public void deleteStaff(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();

        Staff staff = staffRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff", "id", id));

        staff.setDeleted(true);
        staffRepository.save(staff);
        log.info("Soft-deleted staff: {} for tenant: {}", id, tenantId);
    }

    public StaffShiftResponse createShift(UUID staffId, CreateStaffShiftRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        // Verify staff exists
        staffRepository.findByIdAndTenantId(staffId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff", "id", staffId));

        StaffShift shift = staffShiftMapper.toEntity(request);
        shift.setStaffId(staffId);
        shift.setTenantId(tenantId);

        StaffShift saved = staffShiftRepository.save(shift);
        log.info("Created shift: {} for staff: {} tenant: {}", saved.getId(), staffId, tenantId);
        return staffShiftMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<StaffShiftResponse> getShifts(UUID staffId) {
        UUID tenantId = TenantContext.requireTenantId();

        List<StaffShift> shifts = staffShiftRepository.findByStaffIdAndTenantId(staffId, tenantId);
        return shifts.stream()
                .map(staffShiftMapper::toResponse)
                .toList();
    }

    public void deleteShift(UUID shiftId) {
        UUID tenantId = TenantContext.requireTenantId();

        StaffShift shift = staffShiftRepository.findByIdAndTenantId(shiftId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("StaffShift", "id", shiftId));

        shift.setDeleted(true);
        staffShiftRepository.save(shift);
        log.info("Soft-deleted shift: {} for tenant: {}", shiftId, tenantId);
    }

    @Transactional(readOnly = true)
    public List<StaffResponse> getOnDutyStaff(UUID estateId) {
        UUID tenantId = TenantContext.requireTenantId();

        // Get active shifts for the estate (effective date range already filtered by query)
        List<StaffShift> activeShifts = staffShiftRepository.findActiveByEstateIdAndTenantId(estateId, tenantId);

        LocalDateTime now = LocalDateTime.now();
        String currentDay = now.getDayOfWeek().name().substring(0, 3); // MON, TUE, WED, etc.
        LocalTime currentTime = now.toLocalTime();

        // Filter shifts by current day of week and time range
        List<UUID> onDutyStaffIds = activeShifts.stream()
                .filter(shift -> isDayMatch(shift.getDaysOfWeek(), currentDay))
                .filter(shift -> isWithinTimeRange(currentTime, shift.getStartTime(), shift.getEndTime()))
                .map(StaffShift::getStaffId)
                .distinct()
                .toList();

        // Fetch the staff members who are on duty
        return onDutyStaffIds.stream()
                .map(staffId -> staffRepository.findByIdAndTenantId(staffId, tenantId))
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .map(staffMapper::toResponse)
                .collect(Collectors.toList());
    }

    // --- Private helpers ---

    private boolean isDayMatch(String daysOfWeek, String currentDay) {
        if (daysOfWeek == null || daysOfWeek.isBlank()) {
            // If no specific days are set, assume the shift applies every day
            return true;
        }
        String[] allowedDays = daysOfWeek.split(",");
        for (String day : allowedDays) {
            if (day.trim().equalsIgnoreCase(currentDay)) {
                return true;
            }
        }
        return false;
    }

    private boolean isWithinTimeRange(LocalTime currentTime, LocalTime startTime, LocalTime endTime) {
        if (endTime.isAfter(startTime)) {
            // Normal shift: e.g., 08:00 - 16:00
            return !currentTime.isBefore(startTime) && !currentTime.isAfter(endTime);
        } else {
            // Overnight shift: e.g., 22:00 - 06:00
            return !currentTime.isBefore(startTime) || !currentTime.isAfter(endTime);
        }
    }

    private <T> PagedResponse<T> toPagedResponse(Page<?> page, List<T> content) {
        return PagedResponse.<T>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .first(page.isFirst())
                .build();
    }
}
