package com.strataguard.service.governance;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.dto.governance.*;
import com.strataguard.core.entity.Violation;
import com.strataguard.core.enums.ViolationStatus;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.core.util.ViolationMapper;
import com.strataguard.infrastructure.repository.ViolationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ViolationService {

    private final ViolationRepository violationRepository;
    private final ViolationMapper violationMapper;

    public ViolationResponse createViolation(CreateViolationRequest request, String reportedBy, String reportedByName) {
        UUID tenantId = TenantContext.requireTenantId();

        Violation violation = violationMapper.toEntity(request);
        violation.setTenantId(tenantId);
        violation.setReportedBy(reportedBy);
        violation.setReportedByName(reportedByName);

        Violation saved = violationRepository.save(violation);
        log.info("Created violation: {} for tenant: {}", saved.getId(), tenantId);
        return violationMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ViolationResponse getViolation(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();

        Violation violation = violationRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Violation", "id", id));

        return violationMapper.toResponse(violation);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ViolationResponse> getAllViolations(Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<Violation> page = violationRepository.findAllByTenantId(tenantId, pageable);
        return toPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ViolationResponse> getViolationsByEstate(UUID estateId, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<Violation> page = violationRepository.findByEstateIdAndTenantId(estateId, tenantId, pageable);
        return toPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ViolationResponse> getViolationsByUnit(UUID unitId, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<Violation> page = violationRepository.findByUnitIdAndTenantId(unitId, tenantId, pageable);
        return toPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ViolationResponse> getViolationsByStatus(ViolationStatus status, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<Violation> page = violationRepository.findByStatusAndTenantId(status, tenantId, pageable);
        return toPagedResponse(page);
    }

    public ViolationResponse confirmViolation(UUID id, java.math.BigDecimal fineAmount) {
        UUID tenantId = TenantContext.requireTenantId();

        Violation violation = violationRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Violation", "id", id));

        if (violation.getStatus() != ViolationStatus.REPORTED && violation.getStatus() != ViolationStatus.UNDER_REVIEW) {
            throw new IllegalStateException("Violation cannot be confirmed from status: " + violation.getStatus());
        }

        violation.setStatus(ViolationStatus.CONFIRMED);
        if (fineAmount != null) {
            violation.setFineAmount(fineAmount);
        }
        Violation saved = violationRepository.save(violation);
        log.info("Confirmed violation: {} for tenant: {}", id, tenantId);
        return violationMapper.toResponse(saved);
    }

    public ViolationResponse issueFinance(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();

        Violation violation = violationRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Violation", "id", id));

        if (violation.getStatus() != ViolationStatus.CONFIRMED) {
            throw new IllegalStateException("Fine can only be issued for confirmed violations");
        }

        if (violation.getFineAmount() == null) {
            throw new IllegalStateException("Fine amount must be set before issuing");
        }

        violation.setStatus(ViolationStatus.FINED);
        Violation saved = violationRepository.save(violation);
        log.info("Issued fine for violation: {} for tenant: {}", id, tenantId);
        return violationMapper.toResponse(saved);
    }

    public ViolationResponse appealViolation(UUID id, String appealReason) {
        UUID tenantId = TenantContext.requireTenantId();

        Violation violation = violationRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Violation", "id", id));

        if (violation.getStatus() != ViolationStatus.CONFIRMED && violation.getStatus() != ViolationStatus.FINED) {
            throw new IllegalStateException("Only confirmed or fined violations can be appealed");
        }

        violation.setStatus(ViolationStatus.APPEALED);
        violation.setAppealReason(appealReason);
        violation.setAppealedAt(Instant.now());
        Violation saved = violationRepository.save(violation);
        log.info("Appealed violation: {} for tenant: {}", id, tenantId);
        return violationMapper.toResponse(saved);
    }

    public ViolationResponse dismissViolation(UUID id, String resolutionNotes) {
        UUID tenantId = TenantContext.requireTenantId();

        Violation violation = violationRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Violation", "id", id));

        violation.setStatus(ViolationStatus.DISMISSED);
        violation.setResolutionNotes(resolutionNotes);
        violation.setResolvedAt(Instant.now());
        Violation saved = violationRepository.save(violation);
        log.info("Dismissed violation: {} for tenant: {}", id, tenantId);
        return violationMapper.toResponse(saved);
    }

    public ViolationResponse closeViolation(UUID id, String resolutionNotes) {
        UUID tenantId = TenantContext.requireTenantId();

        Violation violation = violationRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Violation", "id", id));

        violation.setStatus(ViolationStatus.CLOSED);
        violation.setResolutionNotes(resolutionNotes);
        violation.setResolvedAt(Instant.now());
        Violation saved = violationRepository.save(violation);
        log.info("Closed violation: {} for tenant: {}", id, tenantId);
        return violationMapper.toResponse(saved);
    }

    private PagedResponse<ViolationResponse> toPagedResponse(Page<Violation> page) {
        List<ViolationResponse> content = page.getContent().stream()
                .map(violationMapper::toResponse)
                .toList();
        return PagedResponse.<ViolationResponse>builder()
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
