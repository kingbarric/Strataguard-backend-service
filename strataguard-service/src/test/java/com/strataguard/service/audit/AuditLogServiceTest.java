package com.strataguard.service.audit;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.audit.AuditLogFilterRequest;
import com.strataguard.core.dto.audit.AuditLogResponse;
import com.strataguard.core.dto.audit.AuditSummaryResponse;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.entity.AuditLog;
import com.strataguard.core.enums.AuditAction;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.core.util.AuditLogMapper;
import com.strataguard.infrastructure.repository.AuditLogRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private AuditLogMapper auditLogMapper;

    @InjectMocks
    private AuditLogService auditLogService;

    private UUID tenantId;
    private UUID logId;
    private AuditLog auditLog;
    private AuditLogResponse auditLogResponse;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        logId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        auditLog = new AuditLog();
        auditLog.setId(logId);
        auditLog.setTenantId(tenantId);
        auditLog.setActorId("user-123");
        auditLog.setActorName("testuser");
        auditLog.setAction(AuditAction.CREATE);
        auditLog.setEntityType("Estate");
        auditLog.setEntityId(UUID.randomUUID().toString());
        auditLog.setTimestamp(Instant.now());
        auditLog.setPreviousHash("GENESIS");
        auditLog.setHash("abc123");

        auditLogResponse = AuditLogResponse.builder()
                .id(logId)
                .actorId("user-123")
                .actorName("testuser")
                .action(AuditAction.CREATE)
                .entityType("Estate")
                .entityId(auditLog.getEntityId())
                .timestamp(auditLog.getTimestamp())
                .build();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void logEvent_shouldSaveAuditLogWithHashChain() {
        when(auditLogRepository.findLastByTenantId(tenantId)).thenReturn(Optional.empty());
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> i.getArgument(0));

        auditLogService.logEvent(tenantId, "user-123", "testuser", AuditAction.CREATE,
                "Estate", "entity-1", null, "{}", "127.0.0.1", "TestAgent", "CREATE Estate");

        verify(auditLogRepository).save(argThat(log -> {
            assertThat(log.getTenantId()).isEqualTo(tenantId);
            assertThat(log.getActorId()).isEqualTo("user-123");
            assertThat(log.getAction()).isEqualTo(AuditAction.CREATE);
            assertThat(log.getPreviousHash()).isEqualTo("GENESIS");
            assertThat(log.getHash()).isNotNull().isNotEmpty();
            return true;
        }));
    }

    @Test
    void logEvent_shouldChainWithPreviousHash() {
        AuditLog previousLog = new AuditLog();
        previousLog.setHash("prev-hash-abc");
        when(auditLogRepository.findLastByTenantId(tenantId)).thenReturn(Optional.of(previousLog));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> i.getArgument(0));

        auditLogService.logEvent(tenantId, "user-123", "testuser", AuditAction.UPDATE,
                "Unit", "entity-2", null, "{}", null, null, "UPDATE Unit");

        verify(auditLogRepository).save(argThat(log -> {
            assertThat(log.getPreviousHash()).isEqualTo("prev-hash-abc");
            return true;
        }));
    }

    @Test
    void getAuditLog_shouldReturnLog() {
        when(auditLogRepository.findByIdAndTenantId(logId, tenantId)).thenReturn(Optional.of(auditLog));
        when(auditLogMapper.toResponse(auditLog)).thenReturn(auditLogResponse);

        AuditLogResponse result = auditLogService.getAuditLog(logId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(logId);
        assertThat(result.getAction()).isEqualTo(AuditAction.CREATE);
    }

    @Test
    void getAuditLog_notFound_shouldThrow() {
        when(auditLogRepository.findByIdAndTenantId(logId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> auditLogService.getAuditLog(logId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAllLogs_shouldReturnPagedResponse() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<AuditLog> page = new PageImpl<>(List.of(auditLog), pageable, 1);
        when(auditLogRepository.findAllByTenantId(tenantId, pageable)).thenReturn(page);
        when(auditLogMapper.toResponse(auditLog)).thenReturn(auditLogResponse);

        PagedResponse<AuditLogResponse> result = auditLogService.getAllLogs(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void getLogsByFilter_withDateRange_shouldFilter() {
        Pageable pageable = PageRequest.of(0, 20);
        Instant start = Instant.now().minusSeconds(3600);
        Instant end = Instant.now();
        AuditLogFilterRequest filter = new AuditLogFilterRequest();
        filter.setStartDate(start);
        filter.setEndDate(end);

        Page<AuditLog> page = new PageImpl<>(List.of(auditLog), pageable, 1);
        when(auditLogRepository.findByDateRange(tenantId, start, end, pageable)).thenReturn(page);
        when(auditLogMapper.toResponse(auditLog)).thenReturn(auditLogResponse);

        PagedResponse<AuditLogResponse> result = auditLogService.getLogsByFilter(filter, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(auditLogRepository).findByDateRange(tenantId, start, end, pageable);
    }

    @Test
    void getLogsByFilter_withActorId_shouldFilter() {
        Pageable pageable = PageRequest.of(0, 20);
        AuditLogFilterRequest filter = new AuditLogFilterRequest();
        filter.setActorId("user-123");

        Page<AuditLog> page = new PageImpl<>(List.of(auditLog), pageable, 1);
        when(auditLogRepository.findByActorId("user-123", tenantId, pageable)).thenReturn(page);
        when(auditLogMapper.toResponse(auditLog)).thenReturn(auditLogResponse);

        PagedResponse<AuditLogResponse> result = auditLogService.getLogsByFilter(filter, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(auditLogRepository).findByActorId("user-123", tenantId, pageable);
    }

    @Test
    void getLogsByFilter_withAction_shouldFilter() {
        Pageable pageable = PageRequest.of(0, 20);
        AuditLogFilterRequest filter = new AuditLogFilterRequest();
        filter.setAction(AuditAction.CREATE);

        Page<AuditLog> page = new PageImpl<>(List.of(auditLog), pageable, 1);
        when(auditLogRepository.findByAction(AuditAction.CREATE, tenantId, pageable)).thenReturn(page);
        when(auditLogMapper.toResponse(auditLog)).thenReturn(auditLogResponse);

        PagedResponse<AuditLogResponse> result = auditLogService.getLogsByFilter(filter, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(auditLogRepository).findByAction(AuditAction.CREATE, tenantId, pageable);
    }

    @Test
    void getLogsByFilter_withEntityTypeAndId_shouldFilter() {
        Pageable pageable = PageRequest.of(0, 20);
        AuditLogFilterRequest filter = new AuditLogFilterRequest();
        filter.setEntityType("Estate");
        filter.setEntityId("estate-1");

        Page<AuditLog> page = new PageImpl<>(List.of(auditLog), pageable, 1);
        when(auditLogRepository.findByEntityTypeAndEntityId("Estate", "estate-1", tenantId, pageable)).thenReturn(page);
        when(auditLogMapper.toResponse(auditLog)).thenReturn(auditLogResponse);

        PagedResponse<AuditLogResponse> result = auditLogService.getLogsByFilter(filter, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(auditLogRepository).findByEntityTypeAndEntityId("Estate", "estate-1", tenantId, pageable);
    }

    @Test
    void getLogsByFilter_withEntityTypeOnly_shouldFilter() {
        Pageable pageable = PageRequest.of(0, 20);
        AuditLogFilterRequest filter = new AuditLogFilterRequest();
        filter.setEntityType("Estate");

        Page<AuditLog> page = new PageImpl<>(List.of(auditLog), pageable, 1);
        when(auditLogRepository.findByEntityType("Estate", tenantId, pageable)).thenReturn(page);
        when(auditLogMapper.toResponse(auditLog)).thenReturn(auditLogResponse);

        PagedResponse<AuditLogResponse> result = auditLogService.getLogsByFilter(filter, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(auditLogRepository).findByEntityType("Estate", tenantId, pageable);
    }

    @Test
    void getLogsByFilter_noFilter_shouldReturnAll() {
        Pageable pageable = PageRequest.of(0, 20);
        AuditLogFilterRequest filter = new AuditLogFilterRequest();

        Page<AuditLog> page = new PageImpl<>(List.of(auditLog), pageable, 1);
        when(auditLogRepository.findAllByTenantId(tenantId, pageable)).thenReturn(page);
        when(auditLogMapper.toResponse(auditLog)).thenReturn(auditLogResponse);

        PagedResponse<AuditLogResponse> result = auditLogService.getLogsByFilter(filter, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(auditLogRepository).findAllByTenantId(tenantId, pageable);
    }

    @Test
    void getEntityHistory_shouldReturnHistory() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<AuditLog> page = new PageImpl<>(List.of(auditLog), pageable, 1);
        when(auditLogRepository.findByEntityTypeAndEntityId("Estate", "entity-1", tenantId, pageable)).thenReturn(page);
        when(auditLogMapper.toResponse(auditLog)).thenReturn(auditLogResponse);

        PagedResponse<AuditLogResponse> result = auditLogService.getEntityHistory("Estate", "entity-1", pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getSummary_shouldReturnSummaryStats() {
        when(auditLogRepository.countByTenantId(tenantId)).thenReturn(50L);
        when(auditLogRepository.countByActionAndTenantId(any(AuditAction.class), eq(tenantId)))
                .thenReturn(0L);
        when(auditLogRepository.countByActionAndTenantId(AuditAction.CREATE, tenantId)).thenReturn(20L);
        when(auditLogRepository.countByActionAndTenantId(AuditAction.UPDATE, tenantId)).thenReturn(25L);
        when(auditLogRepository.countByEntityTypeAndTenantId(anyString(), eq(tenantId))).thenReturn(0L);
        when(auditLogRepository.countByEntityTypeAndTenantId("Estate", tenantId)).thenReturn(10L);

        AuditSummaryResponse result = auditLogService.getSummary();

        assertThat(result.getTotalEvents()).isEqualTo(50);
        assertThat(result.getEventsByAction()).containsEntry("CREATE", 20L);
        assertThat(result.getEventsByAction()).containsEntry("UPDATE", 25L);
        assertThat(result.getEventsByEntityType()).containsEntry("Estate", 10L);
    }

    @Test
    void verifyIntegrity_validChain_shouldReturnTrue() {
        AuditLog log1 = createAuditLog("GENESIS");
        String hash1 = auditLogService.computeHash(log1, "GENESIS");
        log1.setHash(hash1);

        AuditLog log2 = createAuditLog(hash1);
        String hash2 = auditLogService.computeHash(log2, hash1);
        log2.setHash(hash2);

        // findLatest returns newest first, so reverse order
        Page<AuditLog> page = new PageImpl<>(List.of(log2, log1));
        when(auditLogRepository.findLatest(eq(tenantId), any(Pageable.class))).thenReturn(page);

        boolean result = auditLogService.verifyIntegrity(100);

        assertThat(result).isTrue();
    }

    @Test
    void verifyIntegrity_tamperedEntry_shouldReturnFalse() {
        AuditLog log1 = createAuditLog("GENESIS");
        String hash1 = auditLogService.computeHash(log1, "GENESIS");
        log1.setHash(hash1);

        AuditLog log2 = createAuditLog(hash1);
        log2.setHash("tampered-hash");

        Page<AuditLog> page = new PageImpl<>(List.of(log2, log1));
        when(auditLogRepository.findLatest(eq(tenantId), any(Pageable.class))).thenReturn(page);

        boolean result = auditLogService.verifyIntegrity(100);

        assertThat(result).isFalse();
    }

    @Test
    void computeHash_shouldBeDeterministic() {
        AuditLog log = createAuditLog("GENESIS");
        String hash1 = auditLogService.computeHash(log, "GENESIS");
        String hash2 = auditLogService.computeHash(log, "GENESIS");

        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).hasSize(64); // SHA-256 hex string
    }

    @Test
    void computeHash_differentInput_shouldProduceDifferentHash() {
        AuditLog log1 = createAuditLog("GENESIS");
        AuditLog log2 = createAuditLog("GENESIS");
        log2.setActorId("different-user");

        String hash1 = auditLogService.computeHash(log1, "GENESIS");
        String hash2 = auditLogService.computeHash(log2, "GENESIS");

        assertThat(hash1).isNotEqualTo(hash2);
    }

    private AuditLog createAuditLog(String previousHash) {
        AuditLog log = new AuditLog();
        log.setId(UUID.randomUUID());
        log.setTenantId(tenantId);
        log.setActorId("user-123");
        log.setActorName("testuser");
        log.setAction(AuditAction.CREATE);
        log.setEntityType("Estate");
        log.setEntityId(UUID.randomUUID().toString());
        log.setTimestamp(Instant.parse("2024-01-01T00:00:00Z"));
        log.setPreviousHash(previousHash);
        return log;
    }
}
