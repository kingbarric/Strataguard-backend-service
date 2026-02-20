package com.strataguard.service.governance;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.dto.governance.*;
import com.strataguard.core.entity.Violation;
import com.strataguard.core.enums.ViolationStatus;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.core.util.ViolationMapper;
import com.strataguard.infrastructure.repository.ViolationRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ViolationServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID VIOLATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID ESTATE_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID UNIT_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");

    @Mock private ViolationRepository violationRepository;
    @Mock private ViolationMapper violationMapper;

    @InjectMocks
    private ViolationService violationService;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createViolation_success() {
        CreateViolationRequest request = CreateViolationRequest.builder()
                .estateId(ESTATE_ID)
                .unitId(UNIT_ID)
                .ruleViolated("No loud music after 10pm")
                .description("Playing music at 2am")
                .fineAmount(new BigDecimal("5000"))
                .build();

        Violation violation = new Violation();
        violation.setId(VIOLATION_ID);
        ViolationResponse expectedResponse = ViolationResponse.builder().id(VIOLATION_ID).build();

        when(violationMapper.toEntity(request)).thenReturn(violation);
        when(violationRepository.save(any(Violation.class))).thenReturn(violation);
        when(violationMapper.toResponse(violation)).thenReturn(expectedResponse);

        ViolationResponse result = violationService.createViolation(request, "user1", "Admin");

        assertThat(result.getId()).isEqualTo(VIOLATION_ID);
        assertThat(violation.getReportedBy()).isEqualTo("user1");
    }

    @Test
    void getViolation_success() {
        Violation violation = new Violation();
        violation.setId(VIOLATION_ID);
        ViolationResponse expectedResponse = ViolationResponse.builder().id(VIOLATION_ID).build();

        when(violationRepository.findByIdAndTenantId(VIOLATION_ID, TENANT_ID)).thenReturn(Optional.of(violation));
        when(violationMapper.toResponse(violation)).thenReturn(expectedResponse);

        ViolationResponse result = violationService.getViolation(VIOLATION_ID);

        assertThat(result.getId()).isEqualTo(VIOLATION_ID);
    }

    @Test
    void getViolation_notFound() {
        when(violationRepository.findByIdAndTenantId(VIOLATION_ID, TENANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> violationService.getViolation(VIOLATION_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void confirmViolation_success() {
        Violation violation = new Violation();
        violation.setId(VIOLATION_ID);
        violation.setStatus(ViolationStatus.REPORTED);
        ViolationResponse expectedResponse = ViolationResponse.builder().id(VIOLATION_ID).status(ViolationStatus.CONFIRMED).build();

        when(violationRepository.findByIdAndTenantId(VIOLATION_ID, TENANT_ID)).thenReturn(Optional.of(violation));
        when(violationRepository.save(violation)).thenReturn(violation);
        when(violationMapper.toResponse(violation)).thenReturn(expectedResponse);

        ViolationResponse result = violationService.confirmViolation(VIOLATION_ID, new BigDecimal("10000"));

        assertThat(violation.getStatus()).isEqualTo(ViolationStatus.CONFIRMED);
        assertThat(violation.getFineAmount()).isEqualTo(new BigDecimal("10000"));
    }

    @Test
    void confirmViolation_invalidStatus() {
        Violation violation = new Violation();
        violation.setStatus(ViolationStatus.CLOSED);

        when(violationRepository.findByIdAndTenantId(VIOLATION_ID, TENANT_ID)).thenReturn(Optional.of(violation));

        assertThatThrownBy(() -> violationService.confirmViolation(VIOLATION_ID, null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void issueFine_success() {
        Violation violation = new Violation();
        violation.setStatus(ViolationStatus.CONFIRMED);
        violation.setFineAmount(new BigDecimal("5000"));
        ViolationResponse expectedResponse = ViolationResponse.builder().id(VIOLATION_ID).status(ViolationStatus.FINED).build();

        when(violationRepository.findByIdAndTenantId(VIOLATION_ID, TENANT_ID)).thenReturn(Optional.of(violation));
        when(violationRepository.save(violation)).thenReturn(violation);
        when(violationMapper.toResponse(violation)).thenReturn(expectedResponse);

        violationService.issueFinance(VIOLATION_ID);

        assertThat(violation.getStatus()).isEqualTo(ViolationStatus.FINED);
    }

    @Test
    void issueFine_noAmountSet() {
        Violation violation = new Violation();
        violation.setStatus(ViolationStatus.CONFIRMED);
        violation.setFineAmount(null);

        when(violationRepository.findByIdAndTenantId(VIOLATION_ID, TENANT_ID)).thenReturn(Optional.of(violation));

        assertThatThrownBy(() -> violationService.issueFinance(VIOLATION_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Fine amount");
    }

    @Test
    void appealViolation_success() {
        Violation violation = new Violation();
        violation.setStatus(ViolationStatus.FINED);
        ViolationResponse expectedResponse = ViolationResponse.builder().id(VIOLATION_ID).status(ViolationStatus.APPEALED).build();

        when(violationRepository.findByIdAndTenantId(VIOLATION_ID, TENANT_ID)).thenReturn(Optional.of(violation));
        when(violationRepository.save(violation)).thenReturn(violation);
        when(violationMapper.toResponse(violation)).thenReturn(expectedResponse);

        violationService.appealViolation(VIOLATION_ID, "I was not the one playing music");

        assertThat(violation.getStatus()).isEqualTo(ViolationStatus.APPEALED);
        assertThat(violation.getAppealReason()).isEqualTo("I was not the one playing music");
        assertThat(violation.getAppealedAt()).isNotNull();
    }

    @Test
    void appealViolation_invalidStatus() {
        Violation violation = new Violation();
        violation.setStatus(ViolationStatus.REPORTED);

        when(violationRepository.findByIdAndTenantId(VIOLATION_ID, TENANT_ID)).thenReturn(Optional.of(violation));

        assertThatThrownBy(() -> violationService.appealViolation(VIOLATION_ID, "reason"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void dismissViolation_success() {
        Violation violation = new Violation();
        violation.setStatus(ViolationStatus.APPEALED);
        ViolationResponse expectedResponse = ViolationResponse.builder().id(VIOLATION_ID).status(ViolationStatus.DISMISSED).build();

        when(violationRepository.findByIdAndTenantId(VIOLATION_ID, TENANT_ID)).thenReturn(Optional.of(violation));
        when(violationRepository.save(violation)).thenReturn(violation);
        when(violationMapper.toResponse(violation)).thenReturn(expectedResponse);

        violationService.dismissViolation(VIOLATION_ID, "Appeal accepted");

        assertThat(violation.getStatus()).isEqualTo(ViolationStatus.DISMISSED);
        assertThat(violation.getResolvedAt()).isNotNull();
    }

    @Test
    void closeViolation_success() {
        Violation violation = new Violation();
        violation.setStatus(ViolationStatus.FINED);
        ViolationResponse expectedResponse = ViolationResponse.builder().id(VIOLATION_ID).status(ViolationStatus.CLOSED).build();

        when(violationRepository.findByIdAndTenantId(VIOLATION_ID, TENANT_ID)).thenReturn(Optional.of(violation));
        when(violationRepository.save(violation)).thenReturn(violation);
        when(violationMapper.toResponse(violation)).thenReturn(expectedResponse);

        violationService.closeViolation(VIOLATION_ID, "Paid fine");

        assertThat(violation.getStatus()).isEqualTo(ViolationStatus.CLOSED);
        assertThat(violation.getResolvedAt()).isNotNull();
    }

    @Test
    void getAllViolations_success() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Violation> page = new PageImpl<>(List.of(), pageable, 0);

        when(violationRepository.findAllByTenantId(TENANT_ID, pageable)).thenReturn(page);

        PagedResponse<ViolationResponse> result = violationService.getAllViolations(pageable);

        assertThat(result.getContent()).isEmpty();
    }
}
