package com.strataguard.service.resident;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.dto.resident.CreateResidentRequest;
import com.strataguard.core.dto.resident.LinkKeycloakUserRequest;
import com.strataguard.core.dto.resident.ResidentResponse;
import com.strataguard.core.dto.resident.UpdateResidentRequest;
import com.strataguard.core.entity.Resident;
import com.strataguard.core.enums.ResidentStatus;
import com.strataguard.core.exception.DuplicateResourceException;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.core.util.ResidentMapper;
import com.strataguard.infrastructure.repository.ResidentRepository;
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

import java.time.Instant;
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
class ResidentServiceTest {

    @Mock
    private ResidentRepository residentRepository;

    @Mock
    private ResidentMapper residentMapper;

    @InjectMocks
    private ResidentService residentService;

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID RESIDENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");

    private Resident resident;
    private ResidentResponse residentResponse;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID);

        resident = new Resident();
        resident.setId(RESIDENT_ID);
        resident.setTenantId(TENANT_ID);
        resident.setFirstName("John");
        resident.setLastName("Doe");
        resident.setPhone("+2341234567890");
        resident.setEmail("john.doe@example.com");
        resident.setEmergencyContactName("Jane Doe");
        resident.setEmergencyContactPhone("+2349876543210");
        resident.setProfilePhotoUrl("https://example.com/photo.jpg");
        resident.setStatus(ResidentStatus.PENDING_VERIFICATION);
        resident.setActive(true);
        resident.setDeleted(false);
        resident.setCreatedAt(Instant.now());
        resident.setUpdatedAt(Instant.now());

        residentResponse = ResidentResponse.builder()
                .id(RESIDENT_ID)
                .firstName("John")
                .lastName("Doe")
                .phone("+2341234567890")
                .email("john.doe@example.com")
                .emergencyContactName("Jane Doe")
                .emergencyContactPhone("+2349876543210")
                .profilePhotoUrl("https://example.com/photo.jpg")
                .status(ResidentStatus.PENDING_VERIFICATION)
                .active(true)
                .createdAt(resident.getCreatedAt())
                .updatedAt(resident.getUpdatedAt())
                .build();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ========================================================================
    // createResident
    // ========================================================================

    @Nested
    @DisplayName("createResident")
    class CreateResident {

        private CreateResidentRequest createRequest;

        @BeforeEach
        void setUp() {
            createRequest = CreateResidentRequest.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .phone("+2341234567890")
                    .email("john.doe@example.com")
                    .emergencyContactName("Jane Doe")
                    .emergencyContactPhone("+2349876543210")
                    .build();
        }

        @Test
        @DisplayName("should create resident successfully when no duplicate email exists")
        void shouldCreateResidentSuccessfully() {
            when(residentRepository.existsByEmailAndTenantId("john.doe@example.com", TENANT_ID))
                    .thenReturn(false);
            when(residentMapper.toEntity(createRequest)).thenReturn(resident);
            when(residentRepository.save(resident)).thenReturn(resident);
            when(residentMapper.toResponse(resident)).thenReturn(residentResponse);

            ResidentResponse result = residentService.createResident(createRequest);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(RESIDENT_ID);
            assertThat(result.getFirstName()).isEqualTo("John");
            assertThat(result.getLastName()).isEqualTo("Doe");
            assertThat(result.getEmail()).isEqualTo("john.doe@example.com");
            assertThat(result.getStatus()).isEqualTo(ResidentStatus.PENDING_VERIFICATION);
            assertThat(result.isActive()).isTrue();

            verify(residentRepository).existsByEmailAndTenantId("john.doe@example.com", TENANT_ID);
            verify(residentMapper).toEntity(createRequest);
            verify(residentRepository).save(resident);
            verify(residentMapper).toResponse(resident);
        }

        @Test
        @DisplayName("should set tenant ID from TenantContext on created resident")
        void shouldSetTenantIdFromContext() {
            when(residentRepository.existsByEmailAndTenantId("john.doe@example.com", TENANT_ID))
                    .thenReturn(false);
            when(residentMapper.toEntity(createRequest)).thenReturn(resident);
            when(residentRepository.save(resident)).thenReturn(resident);
            when(residentMapper.toResponse(resident)).thenReturn(residentResponse);

            residentService.createResident(createRequest);

            assertThat(resident.getTenantId()).isEqualTo(TENANT_ID);
            verify(residentRepository).save(resident);
        }

        @Test
        @DisplayName("should throw DuplicateResourceException when email already exists for tenant")
        void shouldThrowDuplicateResourceExceptionWhenEmailExists() {
            when(residentRepository.existsByEmailAndTenantId("john.doe@example.com", TENANT_ID))
                    .thenReturn(true);

            assertThatThrownBy(() -> residentService.createResident(createRequest))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("Resident")
                    .hasMessageContaining("email")
                    .hasMessageContaining("john.doe@example.com");

            verify(residentRepository).existsByEmailAndTenantId("john.doe@example.com", TENANT_ID);
            verify(residentRepository, never()).save(any(Resident.class));
            verify(residentMapper, never()).toEntity(any(CreateResidentRequest.class));
        }

        @Test
        @DisplayName("should skip duplicate email check when email is null")
        void shouldSkipDuplicateCheckWhenEmailIsNull() {
            CreateResidentRequest requestWithoutEmail = CreateResidentRequest.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .build();

            when(residentMapper.toEntity(requestWithoutEmail)).thenReturn(resident);
            when(residentRepository.save(resident)).thenReturn(resident);
            when(residentMapper.toResponse(resident)).thenReturn(residentResponse);

            ResidentResponse result = residentService.createResident(requestWithoutEmail);

            assertThat(result).isNotNull();
            verify(residentRepository, never()).existsByEmailAndTenantId(any(), any());
            verify(residentRepository).save(resident);
        }
    }

    // ========================================================================
    // getResident
    // ========================================================================

    @Nested
    @DisplayName("getResident")
    class GetResident {

        @Test
        @DisplayName("should return resident when found by id and tenant")
        void shouldReturnResidentWhenFound() {
            when(residentRepository.findByIdAndTenantId(RESIDENT_ID, TENANT_ID))
                    .thenReturn(Optional.of(resident));
            when(residentMapper.toResponse(resident)).thenReturn(residentResponse);

            ResidentResponse result = residentService.getResident(RESIDENT_ID);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(RESIDENT_ID);
            assertThat(result.getFirstName()).isEqualTo("John");
            assertThat(result.getLastName()).isEqualTo("Doe");

            verify(residentRepository).findByIdAndTenantId(RESIDENT_ID, TENANT_ID);
            verify(residentMapper).toResponse(resident);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when resident not found")
        void shouldThrowResourceNotFoundExceptionWhenNotFound() {
            UUID nonExistentId = UUID.randomUUID();
            when(residentRepository.findByIdAndTenantId(nonExistentId, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> residentService.getResident(nonExistentId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Resident")
                    .hasMessageContaining("id");

            verify(residentRepository).findByIdAndTenantId(nonExistentId, TENANT_ID);
            verify(residentMapper, never()).toResponse(any(Resident.class));
        }
    }

    // ========================================================================
    // getAllResidents
    // ========================================================================

    @Nested
    @DisplayName("getAllResidents")
    class GetAllResidents {

        @Test
        @DisplayName("should return paginated list of residents for tenant")
        void shouldReturnPaginatedResidents() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Resident> page = new PageImpl<>(List.of(resident), pageable, 1);

            when(residentRepository.findAllByTenantId(TENANT_ID, pageable)).thenReturn(page);
            when(residentMapper.toResponse(resident)).thenReturn(residentResponse);

            PagedResponse<ResidentResponse> result = residentService.getAllResidents(pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getFirstName()).isEqualTo("John");
            assertThat(result.getPage()).isZero();
            assertThat(result.getSize()).isEqualTo(10);
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getTotalPages()).isEqualTo(1);
            assertThat(result.isFirst()).isTrue();
            assertThat(result.isLast()).isTrue();

            verify(residentRepository).findAllByTenantId(TENANT_ID, pageable);
        }

        @Test
        @DisplayName("should return empty page when no residents exist for tenant")
        void shouldReturnEmptyPageWhenNoResidents() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Resident> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(residentRepository.findAllByTenantId(TENANT_ID, pageable)).thenReturn(emptyPage);

            PagedResponse<ResidentResponse> result = residentService.getAllResidents(pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("should pass correct pageable parameters to repository")
        void shouldPassCorrectPageableToRepository() {
            Pageable pageable = PageRequest.of(2, 5);
            Page<Resident> page = new PageImpl<>(List.of(), pageable, 0);

            when(residentRepository.findAllByTenantId(TENANT_ID, pageable)).thenReturn(page);

            residentService.getAllResidents(pageable);

            verify(residentRepository).findAllByTenantId(TENANT_ID, pageable);
        }
    }

    // ========================================================================
    // searchResidents
    // ========================================================================

    @Nested
    @DisplayName("searchResidents")
    class SearchResidents {

        @Test
        @DisplayName("should return matching residents for search term")
        void shouldReturnMatchingResidents() {
            String search = "John";
            Pageable pageable = PageRequest.of(0, 10);
            Page<Resident> page = new PageImpl<>(List.of(resident), pageable, 1);

            when(residentRepository.searchByTenantId(TENANT_ID, search, pageable)).thenReturn(page);
            when(residentMapper.toResponse(resident)).thenReturn(residentResponse);

            PagedResponse<ResidentResponse> result = residentService.searchResidents(search, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getFirstName()).isEqualTo("John");

            verify(residentRepository).searchByTenantId(TENANT_ID, search, pageable);
        }

        @Test
        @DisplayName("should return empty page when no residents match search")
        void shouldReturnEmptyPageWhenNoMatch() {
            String search = "NonExistent";
            Pageable pageable = PageRequest.of(0, 10);
            Page<Resident> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(residentRepository.searchByTenantId(TENANT_ID, search, pageable)).thenReturn(emptyPage);

            PagedResponse<ResidentResponse> result = residentService.searchResidents(search, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }
    }

    // ========================================================================
    // getByUserId
    // ========================================================================

    @Nested
    @DisplayName("getByUserId")
    class GetByUserId {

        private static final String USER_ID = "keycloak-user-12345";

        @Test
        @DisplayName("should return resident when found by userId and tenant")
        void shouldReturnResidentWhenFoundByUserId() {
            resident.setUserId(USER_ID);
            ResidentResponse responseWithUserId = ResidentResponse.builder()
                    .id(RESIDENT_ID)
                    .userId(USER_ID)
                    .firstName("John")
                    .lastName("Doe")
                    .email("john.doe@example.com")
                    .status(ResidentStatus.PENDING_VERIFICATION)
                    .active(true)
                    .build();

            when(residentRepository.findByUserIdAndTenantId(USER_ID, TENANT_ID))
                    .thenReturn(Optional.of(resident));
            when(residentMapper.toResponse(resident)).thenReturn(responseWithUserId);

            ResidentResponse result = residentService.getByUserId(USER_ID);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(RESIDENT_ID);
            assertThat(result.getUserId()).isEqualTo(USER_ID);
            assertThat(result.getFirstName()).isEqualTo("John");

            verify(residentRepository).findByUserIdAndTenantId(USER_ID, TENANT_ID);
            verify(residentMapper).toResponse(resident);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when resident not found by userId")
        void shouldThrowResourceNotFoundExceptionWhenNotFoundByUserId() {
            String nonExistentUserId = "non-existent-user-id";
            when(residentRepository.findByUserIdAndTenantId(nonExistentUserId, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> residentService.getByUserId(nonExistentUserId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Resident")
                    .hasMessageContaining("userId");

            verify(residentRepository).findByUserIdAndTenantId(nonExistentUserId, TENANT_ID);
            verify(residentMapper, never()).toResponse(any(Resident.class));
        }
    }

    // ========================================================================
    // updateResident
    // ========================================================================

    @Nested
    @DisplayName("updateResident")
    class UpdateResident {

        private UpdateResidentRequest updateRequest;

        @BeforeEach
        void setUp() {
            updateRequest = UpdateResidentRequest.builder()
                    .firstName("John Updated")
                    .lastName("Doe Updated")
                    .phone("+2340000000000")
                    .email("john.updated@example.com")
                    .emergencyContactName("Updated Contact")
                    .emergencyContactPhone("+2341111111111")
                    .profilePhotoUrl("https://example.com/new-photo.jpg")
                    .status(ResidentStatus.ACTIVE)
                    .active(true)
                    .build();
        }

        @Test
        @DisplayName("should update resident successfully when found")
        void shouldUpdateResidentSuccessfully() {
            Resident updatedResident = new Resident();
            updatedResident.setId(RESIDENT_ID);
            updatedResident.setTenantId(TENANT_ID);
            updatedResident.setFirstName("John Updated");
            updatedResident.setLastName("Doe Updated");
            updatedResident.setPhone("+2340000000000");
            updatedResident.setEmail("john.updated@example.com");
            updatedResident.setEmergencyContactName("Updated Contact");
            updatedResident.setEmergencyContactPhone("+2341111111111");
            updatedResident.setProfilePhotoUrl("https://example.com/new-photo.jpg");
            updatedResident.setStatus(ResidentStatus.ACTIVE);
            updatedResident.setActive(true);

            ResidentResponse updatedResponse = ResidentResponse.builder()
                    .id(RESIDENT_ID)
                    .firstName("John Updated")
                    .lastName("Doe Updated")
                    .phone("+2340000000000")
                    .email("john.updated@example.com")
                    .emergencyContactName("Updated Contact")
                    .emergencyContactPhone("+2341111111111")
                    .profilePhotoUrl("https://example.com/new-photo.jpg")
                    .status(ResidentStatus.ACTIVE)
                    .active(true)
                    .build();

            when(residentRepository.findByIdAndTenantId(RESIDENT_ID, TENANT_ID))
                    .thenReturn(Optional.of(resident));
            when(residentRepository.save(resident)).thenReturn(updatedResident);
            when(residentMapper.toResponse(updatedResident)).thenReturn(updatedResponse);

            ResidentResponse result = residentService.updateResident(RESIDENT_ID, updateRequest);

            assertThat(result).isNotNull();
            assertThat(result.getFirstName()).isEqualTo("John Updated");
            assertThat(result.getLastName()).isEqualTo("Doe Updated");
            assertThat(result.getEmail()).isEqualTo("john.updated@example.com");
            assertThat(result.getStatus()).isEqualTo(ResidentStatus.ACTIVE);

            verify(residentRepository).findByIdAndTenantId(RESIDENT_ID, TENANT_ID);
            verify(residentMapper).updateEntity(updateRequest, resident);
            verify(residentRepository).save(resident);
            verify(residentMapper).toResponse(updatedResident);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when resident not found for update")
        void shouldThrowResourceNotFoundExceptionWhenNotFoundForUpdate() {
            UUID nonExistentId = UUID.randomUUID();
            when(residentRepository.findByIdAndTenantId(nonExistentId, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> residentService.updateResident(nonExistentId, updateRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Resident")
                    .hasMessageContaining("id");

            verify(residentRepository).findByIdAndTenantId(nonExistentId, TENANT_ID);
            verify(residentMapper, never()).updateEntity(any(), any());
            verify(residentRepository, never()).save(any(Resident.class));
        }
    }

    // ========================================================================
    // deleteResident
    // ========================================================================

    @Nested
    @DisplayName("deleteResident")
    class DeleteResident {

        @Test
        @DisplayName("should soft-delete resident by setting deleted=true and active=false")
        void shouldSoftDeleteResident() {
            when(residentRepository.findByIdAndTenantId(RESIDENT_ID, TENANT_ID))
                    .thenReturn(Optional.of(resident));

            residentService.deleteResident(RESIDENT_ID);

            assertThat(resident.isDeleted()).isTrue();
            assertThat(resident.isActive()).isFalse();

            verify(residentRepository).findByIdAndTenantId(RESIDENT_ID, TENANT_ID);
            verify(residentRepository).save(resident);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when resident not found for delete")
        void shouldThrowResourceNotFoundExceptionWhenNotFoundForDelete() {
            UUID nonExistentId = UUID.randomUUID();
            when(residentRepository.findByIdAndTenantId(nonExistentId, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> residentService.deleteResident(nonExistentId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Resident")
                    .hasMessageContaining("id");

            verify(residentRepository).findByIdAndTenantId(nonExistentId, TENANT_ID);
            verify(residentRepository, never()).save(any(Resident.class));
        }
    }

    // ========================================================================
    // linkKeycloakUser
    // ========================================================================

    @Nested
    @DisplayName("linkKeycloakUser")
    class LinkKeycloakUser {

        private static final String KEYCLOAK_USER_ID = "keycloak-user-67890";
        private LinkKeycloakUserRequest linkRequest;

        @BeforeEach
        void setUp() {
            linkRequest = LinkKeycloakUserRequest.builder()
                    .userId(KEYCLOAK_USER_ID)
                    .build();
        }

        @Test
        @DisplayName("should link Keycloak user to resident successfully")
        void shouldLinkKeycloakUserSuccessfully() {
            Resident linkedResident = new Resident();
            linkedResident.setId(RESIDENT_ID);
            linkedResident.setTenantId(TENANT_ID);
            linkedResident.setFirstName("John");
            linkedResident.setLastName("Doe");
            linkedResident.setEmail("john.doe@example.com");
            linkedResident.setUserId(KEYCLOAK_USER_ID);
            linkedResident.setActive(true);

            ResidentResponse linkedResponse = ResidentResponse.builder()
                    .id(RESIDENT_ID)
                    .userId(KEYCLOAK_USER_ID)
                    .firstName("John")
                    .lastName("Doe")
                    .email("john.doe@example.com")
                    .status(ResidentStatus.PENDING_VERIFICATION)
                    .active(true)
                    .build();

            when(residentRepository.findByIdAndTenantId(RESIDENT_ID, TENANT_ID))
                    .thenReturn(Optional.of(resident));
            when(residentRepository.existsByUserIdAndTenantId(KEYCLOAK_USER_ID, TENANT_ID))
                    .thenReturn(false);
            when(residentRepository.save(resident)).thenReturn(linkedResident);
            when(residentMapper.toResponse(linkedResident)).thenReturn(linkedResponse);

            ResidentResponse result = residentService.linkKeycloakUser(RESIDENT_ID, linkRequest);

            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(KEYCLOAK_USER_ID);
            assertThat(result.getId()).isEqualTo(RESIDENT_ID);
            assertThat(resident.getUserId()).isEqualTo(KEYCLOAK_USER_ID);

            verify(residentRepository).findByIdAndTenantId(RESIDENT_ID, TENANT_ID);
            verify(residentRepository).existsByUserIdAndTenantId(KEYCLOAK_USER_ID, TENANT_ID);
            verify(residentRepository).save(resident);
            verify(residentMapper).toResponse(linkedResident);
        }

        @Test
        @DisplayName("should throw DuplicateResourceException when userId already linked to another resident")
        void shouldThrowDuplicateResourceExceptionWhenUserIdAlreadyLinked() {
            when(residentRepository.findByIdAndTenantId(RESIDENT_ID, TENANT_ID))
                    .thenReturn(Optional.of(resident));
            when(residentRepository.existsByUserIdAndTenantId(KEYCLOAK_USER_ID, TENANT_ID))
                    .thenReturn(true);

            assertThatThrownBy(() -> residentService.linkKeycloakUser(RESIDENT_ID, linkRequest))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("Resident")
                    .hasMessageContaining("userId")
                    .hasMessageContaining(KEYCLOAK_USER_ID);

            verify(residentRepository).findByIdAndTenantId(RESIDENT_ID, TENANT_ID);
            verify(residentRepository).existsByUserIdAndTenantId(KEYCLOAK_USER_ID, TENANT_ID);
            verify(residentRepository, never()).save(any(Resident.class));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when resident not found for linking")
        void shouldThrowResourceNotFoundExceptionWhenNotFoundForLinking() {
            UUID nonExistentId = UUID.randomUUID();
            when(residentRepository.findByIdAndTenantId(nonExistentId, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> residentService.linkKeycloakUser(nonExistentId, linkRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Resident")
                    .hasMessageContaining("id");

            verify(residentRepository).findByIdAndTenantId(nonExistentId, TENANT_ID);
            verify(residentRepository, never()).existsByUserIdAndTenantId(any(), any());
            verify(residentRepository, never()).save(any(Resident.class));
        }
    }
}
