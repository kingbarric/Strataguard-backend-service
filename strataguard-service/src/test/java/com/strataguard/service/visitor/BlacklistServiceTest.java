package com.strataguard.service.visitor;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.blacklist.BlacklistResponse;
import com.strataguard.core.dto.blacklist.CreateBlacklistRequest;
import com.strataguard.core.dto.blacklist.UpdateBlacklistRequest;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.entity.Blacklist;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.core.util.BlacklistMapper;
import com.strataguard.infrastructure.repository.BlacklistRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlacklistServiceTest {

    @Mock
    private BlacklistRepository blacklistRepository;

    @Mock
    private BlacklistMapper blacklistMapper;

    @InjectMocks
    private BlacklistService blacklistService;

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID BLACKLIST_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final String CURRENT_USER = "user-keycloak-id-001";

    private Blacklist blacklist;
    private BlacklistResponse blacklistResponse;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID);

        blacklist = new Blacklist();
        blacklist.setId(BLACKLIST_ID);
        blacklist.setTenantId(TENANT_ID);
        blacklist.setName("John Doe");
        blacklist.setPhone("+254712345678");
        blacklist.setPlateNumber("KAA123A");
        blacklist.setReason("Trespassing");
        blacklist.setActive(true);
        blacklist.setAddedBy(CURRENT_USER);
        blacklist.setDeleted(false);
        blacklist.setCreatedAt(Instant.now());
        blacklist.setUpdatedAt(Instant.now());

        blacklistResponse = BlacklistResponse.builder()
                .id(BLACKLIST_ID)
                .name("John Doe")
                .phone("+254712345678")
                .plateNumber("KAA123A")
                .reason("Trespassing")
                .active(true)
                .addedBy(CURRENT_USER)
                .createdAt(blacklist.getCreatedAt())
                .updatedAt(blacklist.getUpdatedAt())
                .build();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    private void mockSecurityContext() {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(CURRENT_USER, null, Collections.emptyList());
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    // ========================================================================
    // create
    // ========================================================================

    @Nested
    @DisplayName("create")
    class Create {

        private CreateBlacklistRequest createRequest;

        @BeforeEach
        void setUp() {
            mockSecurityContext();

            createRequest = CreateBlacklistRequest.builder()
                    .name("John Doe")
                    .phone("+254712345678")
                    .plateNumber("KAA 123A")
                    .reason("Trespassing")
                    .build();
        }

        @Test
        @DisplayName("should create blacklist entry successfully with all fields")
        void shouldCreateBlacklistEntrySuccessfully() {
            when(blacklistMapper.toEntity(createRequest)).thenReturn(blacklist);
            when(blacklistRepository.save(blacklist)).thenReturn(blacklist);
            when(blacklistMapper.toResponse(blacklist)).thenReturn(blacklistResponse);

            BlacklistResponse result = blacklistService.create(createRequest);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(BLACKLIST_ID);
            assertThat(result.getName()).isEqualTo("John Doe");
            assertThat(result.getPhone()).isEqualTo("+254712345678");
            assertThat(result.getPlateNumber()).isEqualTo("KAA123A");
            assertThat(result.getReason()).isEqualTo("Trespassing");
            assertThat(result.isActive()).isTrue();
            assertThat(result.getAddedBy()).isEqualTo(CURRENT_USER);

            verify(blacklistMapper).toEntity(createRequest);
            verify(blacklistRepository).save(blacklist);
            verify(blacklistMapper).toResponse(blacklist);
        }

        @Test
        @DisplayName("should set tenant ID and addedBy from context")
        void shouldSetTenantIdAndAddedBy() {
            when(blacklistMapper.toEntity(createRequest)).thenReturn(blacklist);
            when(blacklistRepository.save(blacklist)).thenReturn(blacklist);
            when(blacklistMapper.toResponse(blacklist)).thenReturn(blacklistResponse);

            blacklistService.create(createRequest);

            assertThat(blacklist.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(blacklist.getAddedBy()).isEqualTo(CURRENT_USER);
            verify(blacklistRepository).save(blacklist);
        }

        @Test
        @DisplayName("should normalize plate number before saving")
        void shouldNormalizePlateNumber() {
            when(blacklistMapper.toEntity(createRequest)).thenReturn(blacklist);
            when(blacklistRepository.save(blacklist)).thenReturn(blacklist);
            when(blacklistMapper.toResponse(blacklist)).thenReturn(blacklistResponse);

            blacklistService.create(createRequest);

            // PlateNumberUtils.normalize("KAA 123A") -> "KAA123A"
            assertThat(blacklist.getPlateNumber()).isEqualTo("KAA123A");
            verify(blacklistRepository).save(blacklist);
        }

        @Test
        @DisplayName("should create blacklist entry with only name provided")
        void shouldCreateWithOnlyName() {
            CreateBlacklistRequest nameOnlyRequest = CreateBlacklistRequest.builder()
                    .name("Jane Doe")
                    .reason("Suspicious behavior")
                    .build();

            Blacklist nameOnlyBlacklist = new Blacklist();
            nameOnlyBlacklist.setId(BLACKLIST_ID);
            nameOnlyBlacklist.setName("Jane Doe");
            nameOnlyBlacklist.setReason("Suspicious behavior");

            BlacklistResponse nameOnlyResponse = BlacklistResponse.builder()
                    .id(BLACKLIST_ID)
                    .name("Jane Doe")
                    .reason("Suspicious behavior")
                    .active(true)
                    .build();

            when(blacklistMapper.toEntity(nameOnlyRequest)).thenReturn(nameOnlyBlacklist);
            when(blacklistRepository.save(nameOnlyBlacklist)).thenReturn(nameOnlyBlacklist);
            when(blacklistMapper.toResponse(nameOnlyBlacklist)).thenReturn(nameOnlyResponse);

            BlacklistResponse result = blacklistService.create(nameOnlyRequest);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Jane Doe");
            verify(blacklistRepository).save(nameOnlyBlacklist);
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when name, phone, and plate are all null")
        void shouldThrowWhenAllIdentifiersNull() {
            CreateBlacklistRequest emptyRequest = CreateBlacklistRequest.builder()
                    .reason("Some reason")
                    .build();

            assertThatThrownBy(() -> blacklistService.create(emptyRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("At least one of name, phone, or plate number is required");

            verify(blacklistRepository, never()).save(any(Blacklist.class));
            verify(blacklistMapper, never()).toEntity(any(CreateBlacklistRequest.class));
        }

        @Test
        @DisplayName("should create blacklist entry with only phone provided")
        void shouldCreateWithOnlyPhone() {
            CreateBlacklistRequest phoneOnlyRequest = CreateBlacklistRequest.builder()
                    .phone("+254700000000")
                    .reason("Harassing residents")
                    .build();

            Blacklist phoneOnlyBlacklist = new Blacklist();
            phoneOnlyBlacklist.setId(BLACKLIST_ID);

            when(blacklistMapper.toEntity(phoneOnlyRequest)).thenReturn(phoneOnlyBlacklist);
            when(blacklistRepository.save(phoneOnlyBlacklist)).thenReturn(phoneOnlyBlacklist);
            when(blacklistMapper.toResponse(phoneOnlyBlacklist)).thenReturn(blacklistResponse);

            BlacklistResponse result = blacklistService.create(phoneOnlyRequest);

            assertThat(result).isNotNull();
            verify(blacklistRepository).save(phoneOnlyBlacklist);
        }

        @Test
        @DisplayName("should create blacklist entry with only plate number provided")
        void shouldCreateWithOnlyPlateNumber() {
            CreateBlacklistRequest plateOnlyRequest = CreateBlacklistRequest.builder()
                    .plateNumber("KBZ 999Z")
                    .reason("Unauthorized parking")
                    .build();

            Blacklist plateOnlyBlacklist = new Blacklist();
            plateOnlyBlacklist.setId(BLACKLIST_ID);

            when(blacklistMapper.toEntity(plateOnlyRequest)).thenReturn(plateOnlyBlacklist);
            when(blacklistRepository.save(plateOnlyBlacklist)).thenReturn(plateOnlyBlacklist);
            when(blacklistMapper.toResponse(plateOnlyBlacklist)).thenReturn(blacklistResponse);

            BlacklistResponse result = blacklistService.create(plateOnlyRequest);

            assertThat(result).isNotNull();
            assertThat(plateOnlyBlacklist.getPlateNumber()).isEqualTo("KBZ999Z");
            verify(blacklistRepository).save(plateOnlyBlacklist);
        }
    }

    // ========================================================================
    // getById
    // ========================================================================

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @DisplayName("should return blacklist entry when found by id and tenant")
        void shouldReturnBlacklistEntryWhenFound() {
            when(blacklistRepository.findByIdAndTenantId(BLACKLIST_ID, TENANT_ID))
                    .thenReturn(Optional.of(blacklist));
            when(blacklistMapper.toResponse(blacklist)).thenReturn(blacklistResponse);

            BlacklistResponse result = blacklistService.getById(BLACKLIST_ID);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(BLACKLIST_ID);
            assertThat(result.getName()).isEqualTo("John Doe");
            assertThat(result.getPhone()).isEqualTo("+254712345678");
            assertThat(result.getPlateNumber()).isEqualTo("KAA123A");
            assertThat(result.getReason()).isEqualTo("Trespassing");

            verify(blacklistRepository).findByIdAndTenantId(BLACKLIST_ID, TENANT_ID);
            verify(blacklistMapper).toResponse(blacklist);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when blacklist entry not found")
        void shouldThrowResourceNotFoundExceptionWhenNotFound() {
            UUID nonExistentId = UUID.randomUUID();
            when(blacklistRepository.findByIdAndTenantId(nonExistentId, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> blacklistService.getById(nonExistentId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Blacklist")
                    .hasMessageContaining("id");

            verify(blacklistRepository).findByIdAndTenantId(nonExistentId, TENANT_ID);
            verify(blacklistMapper, never()).toResponse(any(Blacklist.class));
        }
    }

    // ========================================================================
    // getAll
    // ========================================================================

    @Nested
    @DisplayName("getAll")
    class GetAll {

        @Test
        @DisplayName("should return paginated list of blacklist entries for tenant")
        void shouldReturnPaginatedBlacklistEntries() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Blacklist> page = new PageImpl<>(List.of(blacklist), pageable, 1);

            when(blacklistRepository.findAllByTenantId(TENANT_ID, pageable)).thenReturn(page);
            when(blacklistMapper.toResponse(blacklist)).thenReturn(blacklistResponse);

            PagedResponse<BlacklistResponse> result = blacklistService.getAll(pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("John Doe");
            assertThat(result.getPage()).isZero();
            assertThat(result.getSize()).isEqualTo(10);
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getTotalPages()).isEqualTo(1);
            assertThat(result.isFirst()).isTrue();
            assertThat(result.isLast()).isTrue();

            verify(blacklistRepository).findAllByTenantId(TENANT_ID, pageable);
        }

        @Test
        @DisplayName("should return empty page when no blacklist entries exist")
        void shouldReturnEmptyPageWhenNoEntries() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Blacklist> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(blacklistRepository.findAllByTenantId(TENANT_ID, pageable)).thenReturn(emptyPage);

            PagedResponse<BlacklistResponse> result = blacklistService.getAll(pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("should pass correct pageable parameters to repository")
        void shouldPassCorrectPageableToRepository() {
            Pageable pageable = PageRequest.of(2, 5);
            Page<Blacklist> page = new PageImpl<>(List.of(), pageable, 0);

            when(blacklistRepository.findAllByTenantId(TENANT_ID, pageable)).thenReturn(page);

            blacklistService.getAll(pageable);

            verify(blacklistRepository).findAllByTenantId(TENANT_ID, pageable);
        }
    }

    // ========================================================================
    // update
    // ========================================================================

    @Nested
    @DisplayName("update")
    class Update {

        private UpdateBlacklistRequest updateRequest;

        @BeforeEach
        void setUp() {
            updateRequest = UpdateBlacklistRequest.builder()
                    .name("John Doe Updated")
                    .phone("+254799999999")
                    .plateNumber("KBB 456B")
                    .reason("Updated reason")
                    .active(true)
                    .build();
        }

        @Test
        @DisplayName("should update blacklist entry successfully when found")
        void shouldUpdateBlacklistEntrySuccessfully() {
            Blacklist updatedBlacklist = new Blacklist();
            updatedBlacklist.setId(BLACKLIST_ID);
            updatedBlacklist.setTenantId(TENANT_ID);
            updatedBlacklist.setName("John Doe Updated");
            updatedBlacklist.setPhone("+254799999999");
            updatedBlacklist.setPlateNumber("KBB456B");
            updatedBlacklist.setReason("Updated reason");
            updatedBlacklist.setActive(true);
            updatedBlacklist.setAddedBy(CURRENT_USER);

            BlacklistResponse updatedResponse = BlacklistResponse.builder()
                    .id(BLACKLIST_ID)
                    .name("John Doe Updated")
                    .phone("+254799999999")
                    .plateNumber("KBB456B")
                    .reason("Updated reason")
                    .active(true)
                    .addedBy(CURRENT_USER)
                    .build();

            when(blacklistRepository.findByIdAndTenantId(BLACKLIST_ID, TENANT_ID))
                    .thenReturn(Optional.of(blacklist));
            when(blacklistRepository.save(blacklist)).thenReturn(updatedBlacklist);
            when(blacklistMapper.toResponse(updatedBlacklist)).thenReturn(updatedResponse);

            BlacklistResponse result = blacklistService.update(BLACKLIST_ID, updateRequest);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("John Doe Updated");
            assertThat(result.getPhone()).isEqualTo("+254799999999");
            assertThat(result.getPlateNumber()).isEqualTo("KBB456B");
            assertThat(result.getReason()).isEqualTo("Updated reason");

            verify(blacklistRepository).findByIdAndTenantId(BLACKLIST_ID, TENANT_ID);
            verify(blacklistMapper).updateEntity(updateRequest, blacklist);
            verify(blacklistRepository).save(blacklist);
            verify(blacklistMapper).toResponse(updatedBlacklist);
        }

        @Test
        @DisplayName("should normalize plate number on update")
        void shouldNormalizePlateNumberOnUpdate() {
            when(blacklistRepository.findByIdAndTenantId(BLACKLIST_ID, TENANT_ID))
                    .thenReturn(Optional.of(blacklist));
            when(blacklistRepository.save(blacklist)).thenReturn(blacklist);
            when(blacklistMapper.toResponse(blacklist)).thenReturn(blacklistResponse);

            blacklistService.update(BLACKLIST_ID, updateRequest);

            // PlateNumberUtils.normalize("KBB 456B") -> "KBB456B"
            assertThat(updateRequest.getPlateNumber()).isEqualTo("KBB456B");
        }

        @Test
        @DisplayName("should not normalize plate number when null on update")
        void shouldNotNormalizePlateNumberWhenNull() {
            UpdateBlacklistRequest noPlateRequest = UpdateBlacklistRequest.builder()
                    .name("Updated Name")
                    .reason("Updated reason")
                    .build();

            when(blacklistRepository.findByIdAndTenantId(BLACKLIST_ID, TENANT_ID))
                    .thenReturn(Optional.of(blacklist));
            when(blacklistRepository.save(blacklist)).thenReturn(blacklist);
            when(blacklistMapper.toResponse(blacklist)).thenReturn(blacklistResponse);

            blacklistService.update(BLACKLIST_ID, noPlateRequest);

            assertThat(noPlateRequest.getPlateNumber()).isNull();
            verify(blacklistMapper).updateEntity(noPlateRequest, blacklist);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when blacklist entry not found for update")
        void shouldThrowResourceNotFoundExceptionWhenNotFoundForUpdate() {
            UUID nonExistentId = UUID.randomUUID();
            when(blacklistRepository.findByIdAndTenantId(nonExistentId, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> blacklistService.update(nonExistentId, updateRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Blacklist")
                    .hasMessageContaining("id");

            verify(blacklistRepository).findByIdAndTenantId(nonExistentId, TENANT_ID);
            verify(blacklistMapper, never()).updateEntity(any(), any());
            verify(blacklistRepository, never()).save(any(Blacklist.class));
        }
    }

    // ========================================================================
    // deactivate
    // ========================================================================

    @Nested
    @DisplayName("deactivate")
    class Deactivate {

        @Test
        @DisplayName("should deactivate blacklist entry by setting active=false")
        void shouldDeactivateBlacklistEntry() {
            when(blacklistRepository.findByIdAndTenantId(BLACKLIST_ID, TENANT_ID))
                    .thenReturn(Optional.of(blacklist));

            blacklistService.deactivate(BLACKLIST_ID);

            assertThat(blacklist.isActive()).isFalse();

            verify(blacklistRepository).findByIdAndTenantId(BLACKLIST_ID, TENANT_ID);
            verify(blacklistRepository).save(blacklist);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when blacklist entry not found for deactivation")
        void shouldThrowResourceNotFoundExceptionWhenNotFoundForDeactivation() {
            UUID nonExistentId = UUID.randomUUID();
            when(blacklistRepository.findByIdAndTenantId(nonExistentId, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> blacklistService.deactivate(nonExistentId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Blacklist")
                    .hasMessageContaining("id");

            verify(blacklistRepository).findByIdAndTenantId(nonExistentId, TENANT_ID);
            verify(blacklistRepository, never()).save(any(Blacklist.class));
        }
    }

    // ========================================================================
    // delete
    // ========================================================================

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should soft-delete blacklist entry by setting deleted=true")
        void shouldSoftDeleteBlacklistEntry() {
            when(blacklistRepository.findByIdAndTenantId(BLACKLIST_ID, TENANT_ID))
                    .thenReturn(Optional.of(blacklist));

            blacklistService.delete(BLACKLIST_ID);

            assertThat(blacklist.isDeleted()).isTrue();

            verify(blacklistRepository).findByIdAndTenantId(BLACKLIST_ID, TENANT_ID);
            verify(blacklistRepository).save(blacklist);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when blacklist entry not found for delete")
        void shouldThrowResourceNotFoundExceptionWhenNotFoundForDelete() {
            UUID nonExistentId = UUID.randomUUID();
            when(blacklistRepository.findByIdAndTenantId(nonExistentId, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> blacklistService.delete(nonExistentId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Blacklist")
                    .hasMessageContaining("id");

            verify(blacklistRepository).findByIdAndTenantId(nonExistentId, TENANT_ID);
            verify(blacklistRepository, never()).save(any(Blacklist.class));
        }
    }

    // ========================================================================
    // search
    // ========================================================================

    @Nested
    @DisplayName("search")
    class Search {

        @Test
        @DisplayName("should return matching blacklist entries for search query")
        void shouldReturnMatchingBlacklistEntries() {
            String query = "John";
            Pageable pageable = PageRequest.of(0, 10);
            Page<Blacklist> page = new PageImpl<>(List.of(blacklist), pageable, 1);

            when(blacklistRepository.search(query, TENANT_ID, pageable)).thenReturn(page);
            when(blacklistMapper.toResponse(blacklist)).thenReturn(blacklistResponse);

            PagedResponse<BlacklistResponse> result = blacklistService.search(query, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("John Doe");
            assertThat(result.getTotalElements()).isEqualTo(1);

            verify(blacklistRepository).search(query, TENANT_ID, pageable);
        }

        @Test
        @DisplayName("should return empty page when no blacklist entries match search")
        void shouldReturnEmptyPageWhenNoMatch() {
            String query = "NonExistent";
            Pageable pageable = PageRequest.of(0, 10);
            Page<Blacklist> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(blacklistRepository.search(query, TENANT_ID, pageable)).thenReturn(emptyPage);

            PagedResponse<BlacklistResponse> result = blacklistService.search(query, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }
    }

    // ========================================================================
    // isPlateBlacklisted
    // ========================================================================

    @Nested
    @DisplayName("isPlateBlacklisted")
    class IsPlateBlacklisted {

        @Test
        @DisplayName("should return true when plate number is blacklisted")
        void shouldReturnTrueWhenPlateBlacklisted() {
            String plateNumber = "KAA 123A";
            when(blacklistRepository.isPlateBlacklisted("KAA123A", TENANT_ID)).thenReturn(true);

            boolean result = blacklistService.isPlateBlacklisted(plateNumber);

            assertThat(result).isTrue();
            verify(blacklistRepository).isPlateBlacklisted("KAA123A", TENANT_ID);
        }

        @Test
        @DisplayName("should return false when plate number is not blacklisted")
        void shouldReturnFalseWhenPlateNotBlacklisted() {
            String plateNumber = "KZZ 999Z";
            when(blacklistRepository.isPlateBlacklisted("KZZ999Z", TENANT_ID)).thenReturn(false);

            boolean result = blacklistService.isPlateBlacklisted(plateNumber);

            assertThat(result).isFalse();
            verify(blacklistRepository).isPlateBlacklisted("KZZ999Z", TENANT_ID);
        }

        @Test
        @DisplayName("should normalize plate number before checking blacklist")
        void shouldNormalizePlateNumberBeforeCheck() {
            String plateNumber = "kaa-123-a";
            when(blacklistRepository.isPlateBlacklisted("KAA123A", TENANT_ID)).thenReturn(true);

            blacklistService.isPlateBlacklisted(plateNumber);

            verify(blacklistRepository).isPlateBlacklisted("KAA123A", TENANT_ID);
        }
    }

    // ========================================================================
    // isPhoneBlacklisted
    // ========================================================================

    @Nested
    @DisplayName("isPhoneBlacklisted")
    class IsPhoneBlacklisted {

        @Test
        @DisplayName("should return true when phone number is blacklisted")
        void shouldReturnTrueWhenPhoneBlacklisted() {
            String phone = "+254712345678";
            when(blacklistRepository.isPhoneBlacklisted(phone, TENANT_ID)).thenReturn(true);

            boolean result = blacklistService.isPhoneBlacklisted(phone);

            assertThat(result).isTrue();
            verify(blacklistRepository).isPhoneBlacklisted(phone, TENANT_ID);
        }

        @Test
        @DisplayName("should return false when phone number is not blacklisted")
        void shouldReturnFalseWhenPhoneNotBlacklisted() {
            String phone = "+254700000000";
            when(blacklistRepository.isPhoneBlacklisted(phone, TENANT_ID)).thenReturn(false);

            boolean result = blacklistService.isPhoneBlacklisted(phone);

            assertThat(result).isFalse();
            verify(blacklistRepository).isPhoneBlacklisted(phone, TENANT_ID);
        }
    }
}
