package com.strataguard.service.governance;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.dto.governance.*;
import com.strataguard.core.entity.Complaint;
import com.strataguard.core.enums.ComplaintCategory;
import com.strataguard.core.enums.ComplaintStatus;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.core.util.ComplaintMapper;
import com.strataguard.infrastructure.repository.ComplaintRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComplaintServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID COMPLAINT_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID ESTATE_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID RESIDENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");

    @Mock private ComplaintRepository complaintRepository;
    @Mock private ComplaintMapper complaintMapper;

    @InjectMocks
    private ComplaintService complaintService;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createComplaint_success() {
        CreateComplaintRequest request = CreateComplaintRequest.builder()
                .estateId(ESTATE_ID)
                .title("Noise complaint")
                .description("Loud music at 2am")
                .category(ComplaintCategory.NOISE)
                .anonymous(false)
                .build();

        Complaint complaint = new Complaint();
        complaint.setId(COMPLAINT_ID);
        ComplaintResponse expectedResponse = ComplaintResponse.builder().id(COMPLAINT_ID).title("Noise complaint").build();

        when(complaintMapper.toEntity(request)).thenReturn(complaint);
        when(complaintRepository.save(any(Complaint.class))).thenReturn(complaint);
        when(complaintMapper.toResponse(complaint)).thenReturn(expectedResponse);

        ComplaintResponse result = complaintService.createComplaint(RESIDENT_ID, request);

        assertThat(result.getId()).isEqualTo(COMPLAINT_ID);
        assertThat(complaint.getResidentId()).isEqualTo(RESIDENT_ID);
    }

    @Test
    void getComplaint_success() {
        Complaint complaint = new Complaint();
        complaint.setId(COMPLAINT_ID);
        ComplaintResponse expectedResponse = ComplaintResponse.builder().id(COMPLAINT_ID).build();

        when(complaintRepository.findByIdAndTenantId(COMPLAINT_ID, TENANT_ID)).thenReturn(Optional.of(complaint));
        when(complaintMapper.toResponse(complaint)).thenReturn(expectedResponse);

        ComplaintResponse result = complaintService.getComplaint(COMPLAINT_ID);

        assertThat(result.getId()).isEqualTo(COMPLAINT_ID);
    }

    @Test
    void getComplaint_notFound() {
        when(complaintRepository.findByIdAndTenantId(COMPLAINT_ID, TENANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> complaintService.getComplaint(COMPLAINT_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void acknowledgeComplaint_success() {
        Complaint complaint = new Complaint();
        complaint.setId(COMPLAINT_ID);
        complaint.setStatus(ComplaintStatus.OPEN);
        ComplaintResponse expectedResponse = ComplaintResponse.builder().id(COMPLAINT_ID).status(ComplaintStatus.ACKNOWLEDGED).build();

        when(complaintRepository.findByIdAndTenantId(COMPLAINT_ID, TENANT_ID)).thenReturn(Optional.of(complaint));
        when(complaintRepository.save(complaint)).thenReturn(complaint);
        when(complaintMapper.toResponse(complaint)).thenReturn(expectedResponse);

        complaintService.acknowledgeComplaint(COMPLAINT_ID);

        assertThat(complaint.getStatus()).isEqualTo(ComplaintStatus.ACKNOWLEDGED);
    }

    @Test
    void acknowledgeComplaint_notOpen() {
        Complaint complaint = new Complaint();
        complaint.setStatus(ComplaintStatus.RESOLVED);

        when(complaintRepository.findByIdAndTenantId(COMPLAINT_ID, TENANT_ID)).thenReturn(Optional.of(complaint));

        assertThatThrownBy(() -> complaintService.acknowledgeComplaint(COMPLAINT_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("open");
    }

    @Test
    void assignComplaint_success() {
        Complaint complaint = new Complaint();
        complaint.setId(COMPLAINT_ID);
        complaint.setStatus(ComplaintStatus.OPEN);
        ComplaintResponse expectedResponse = ComplaintResponse.builder().id(COMPLAINT_ID).status(ComplaintStatus.IN_PROGRESS).build();

        when(complaintRepository.findByIdAndTenantId(COMPLAINT_ID, TENANT_ID)).thenReturn(Optional.of(complaint));
        when(complaintRepository.save(complaint)).thenReturn(complaint);
        when(complaintMapper.toResponse(complaint)).thenReturn(expectedResponse);

        complaintService.assignComplaint(COMPLAINT_ID, "staff1", "John Staff");

        assertThat(complaint.getStatus()).isEqualTo(ComplaintStatus.IN_PROGRESS);
        assertThat(complaint.getAssignedTo()).isEqualTo("staff1");
        assertThat(complaint.getAssignedToName()).isEqualTo("John Staff");
    }

    @Test
    void resolveComplaint_success() {
        Complaint complaint = new Complaint();
        complaint.setId(COMPLAINT_ID);
        complaint.setStatus(ComplaintStatus.IN_PROGRESS);
        ComplaintResponse expectedResponse = ComplaintResponse.builder().id(COMPLAINT_ID).status(ComplaintStatus.RESOLVED).build();

        when(complaintRepository.findByIdAndTenantId(COMPLAINT_ID, TENANT_ID)).thenReturn(Optional.of(complaint));
        when(complaintRepository.save(complaint)).thenReturn(complaint);
        when(complaintMapper.toResponse(complaint)).thenReturn(expectedResponse);

        complaintService.resolveComplaint(COMPLAINT_ID, "Spoke to the neighbor");

        assertThat(complaint.getStatus()).isEqualTo(ComplaintStatus.RESOLVED);
        assertThat(complaint.getResponseNotes()).isEqualTo("Spoke to the neighbor");
        assertThat(complaint.getResolvedAt()).isNotNull();
    }

    @Test
    void closeComplaint_success() {
        Complaint complaint = new Complaint();
        complaint.setStatus(ComplaintStatus.RESOLVED);
        ComplaintResponse expectedResponse = ComplaintResponse.builder().id(COMPLAINT_ID).status(ComplaintStatus.CLOSED).build();

        when(complaintRepository.findByIdAndTenantId(COMPLAINT_ID, TENANT_ID)).thenReturn(Optional.of(complaint));
        when(complaintRepository.save(complaint)).thenReturn(complaint);
        when(complaintMapper.toResponse(complaint)).thenReturn(expectedResponse);

        complaintService.closeComplaint(COMPLAINT_ID);

        assertThat(complaint.getStatus()).isEqualTo(ComplaintStatus.CLOSED);
    }

    @Test
    void rejectComplaint_success() {
        Complaint complaint = new Complaint();
        complaint.setStatus(ComplaintStatus.OPEN);
        ComplaintResponse expectedResponse = ComplaintResponse.builder().id(COMPLAINT_ID).status(ComplaintStatus.REJECTED).build();

        when(complaintRepository.findByIdAndTenantId(COMPLAINT_ID, TENANT_ID)).thenReturn(Optional.of(complaint));
        when(complaintRepository.save(complaint)).thenReturn(complaint);
        when(complaintMapper.toResponse(complaint)).thenReturn(expectedResponse);

        complaintService.rejectComplaint(COMPLAINT_ID, "Not enough evidence");

        assertThat(complaint.getStatus()).isEqualTo(ComplaintStatus.REJECTED);
        assertThat(complaint.getResponseNotes()).isEqualTo("Not enough evidence");
    }

    @Test
    void getAllComplaints_success() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Complaint> page = new PageImpl<>(List.of(), pageable, 0);

        when(complaintRepository.findAllByTenantId(TENANT_ID, pageable)).thenReturn(page);

        PagedResponse<ComplaintResponse> result = complaintService.getAllComplaints(pageable);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void getMyComplaints_success() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Complaint> page = new PageImpl<>(List.of(), pageable, 0);

        when(complaintRepository.findByResidentIdAndTenantId(RESIDENT_ID, TENANT_ID, pageable)).thenReturn(page);

        PagedResponse<ComplaintResponse> result = complaintService.getMyComplaints(RESIDENT_ID, pageable);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void deleteComplaint_success() {
        Complaint complaint = new Complaint();
        complaint.setId(COMPLAINT_ID);

        when(complaintRepository.findByIdAndTenantId(COMPLAINT_ID, TENANT_ID)).thenReturn(Optional.of(complaint));

        complaintService.deleteComplaint(COMPLAINT_ID);

        assertThat(complaint.isDeleted()).isTrue();
        verify(complaintRepository).save(complaint);
    }
}
