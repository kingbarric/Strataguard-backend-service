package com.strataguard.service.governance;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.dto.governance.*;
import com.strataguard.core.entity.Complaint;
import com.strataguard.core.enums.ComplaintStatus;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.core.util.ComplaintMapper;
import com.strataguard.infrastructure.repository.ComplaintRepository;
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
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final ComplaintMapper complaintMapper;

    public ComplaintResponse createComplaint(UUID residentId, CreateComplaintRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        Complaint complaint = complaintMapper.toEntity(request);
        complaint.setTenantId(tenantId);
        complaint.setResidentId(residentId);

        Complaint saved = complaintRepository.save(complaint);
        log.info("Created complaint: {} for tenant: {}", saved.getId(), tenantId);
        return complaintMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ComplaintResponse getComplaint(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();

        Complaint complaint = complaintRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint", "id", id));

        return complaintMapper.toResponse(complaint);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ComplaintResponse> getAllComplaints(Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<Complaint> page = complaintRepository.findAllByTenantId(tenantId, pageable);
        return toPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ComplaintResponse> getComplaintsByEstate(UUID estateId, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<Complaint> page = complaintRepository.findByEstateIdAndTenantId(estateId, tenantId, pageable);
        return toPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ComplaintResponse> getMyComplaints(UUID residentId, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<Complaint> page = complaintRepository.findByResidentIdAndTenantId(residentId, tenantId, pageable);
        return toPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ComplaintResponse> getComplaintsByStatus(ComplaintStatus status, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<Complaint> page = complaintRepository.findByStatusAndTenantId(status, tenantId, pageable);
        return toPagedResponse(page);
    }

    public ComplaintResponse acknowledgeComplaint(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();

        Complaint complaint = complaintRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint", "id", id));

        if (complaint.getStatus() != ComplaintStatus.OPEN) {
            throw new IllegalStateException("Only open complaints can be acknowledged");
        }

        complaint.setStatus(ComplaintStatus.ACKNOWLEDGED);
        Complaint saved = complaintRepository.save(complaint);
        log.info("Acknowledged complaint: {} for tenant: {}", id, tenantId);
        return complaintMapper.toResponse(saved);
    }

    public ComplaintResponse assignComplaint(UUID id, String assignedTo, String assignedToName) {
        UUID tenantId = TenantContext.requireTenantId();

        Complaint complaint = complaintRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint", "id", id));

        complaint.setStatus(ComplaintStatus.IN_PROGRESS);
        complaint.setAssignedTo(assignedTo);
        complaint.setAssignedToName(assignedToName);
        Complaint saved = complaintRepository.save(complaint);
        log.info("Assigned complaint: {} to {} for tenant: {}", id, assignedTo, tenantId);
        return complaintMapper.toResponse(saved);
    }

    public ComplaintResponse resolveComplaint(UUID id, String responseNotes) {
        UUID tenantId = TenantContext.requireTenantId();

        Complaint complaint = complaintRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint", "id", id));

        complaint.setStatus(ComplaintStatus.RESOLVED);
        complaint.setResponseNotes(responseNotes);
        complaint.setResolvedAt(Instant.now());
        Complaint saved = complaintRepository.save(complaint);
        log.info("Resolved complaint: {} for tenant: {}", id, tenantId);
        return complaintMapper.toResponse(saved);
    }

    public ComplaintResponse closeComplaint(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();

        Complaint complaint = complaintRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint", "id", id));

        complaint.setStatus(ComplaintStatus.CLOSED);
        Complaint saved = complaintRepository.save(complaint);
        log.info("Closed complaint: {} for tenant: {}", id, tenantId);
        return complaintMapper.toResponse(saved);
    }

    public ComplaintResponse rejectComplaint(UUID id, String responseNotes) {
        UUID tenantId = TenantContext.requireTenantId();

        Complaint complaint = complaintRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint", "id", id));

        complaint.setStatus(ComplaintStatus.REJECTED);
        complaint.setResponseNotes(responseNotes);
        Complaint saved = complaintRepository.save(complaint);
        log.info("Rejected complaint: {} for tenant: {}", id, tenantId);
        return complaintMapper.toResponse(saved);
    }

    public void deleteComplaint(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();

        Complaint complaint = complaintRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint", "id", id));

        complaint.setDeleted(true);
        complaintRepository.save(complaint);
        log.info("Soft-deleted complaint: {} for tenant: {}", id, tenantId);
    }

    private PagedResponse<ComplaintResponse> toPagedResponse(Page<Complaint> page) {
        List<ComplaintResponse> content = page.getContent().stream()
                .map(complaintMapper::toResponse)
                .toList();
        return PagedResponse.<ComplaintResponse>builder()
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
