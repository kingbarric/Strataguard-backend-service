package com.strataguard.api.controller;

import com.strataguard.core.dto.common.ApiResponse;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.dto.resident.CreateResidentRequest;
import com.strataguard.core.dto.resident.LinkKeycloakUserRequest;
import com.strataguard.core.dto.resident.ResidentResponse;
import com.strataguard.core.dto.resident.UpdateResidentRequest;
import com.strataguard.service.resident.ResidentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResidentControllerTest {

    private static final UUID RESIDENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Pageable PAGEABLE = PageRequest.of(0, 20);

    @Mock
    private ResidentService residentService;

    @InjectMocks
    private ResidentController residentController;

    @Nested
    @DisplayName("POST / - Create Resident")
    class CreateResident {

        @Test
        @DisplayName("Should create resident and return 201 CREATED")
        void shouldCreateResidentAndReturn201() {
            CreateResidentRequest request = CreateResidentRequest.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .email("john.doe@example.com")
                    .build();
            ResidentResponse expectedResponse = ResidentResponse.builder()
                    .id(RESIDENT_ID)
                    .firstName("John")
                    .lastName("Doe")
                    .build();
            when(residentService.createResident(request)).thenReturn(expectedResponse);

            ResponseEntity<ApiResponse<ResidentResponse>> result = residentController.createResident(request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getMessage()).isEqualTo("Resident created successfully");
            assertThat(result.getBody().getData()).isEqualTo(expectedResponse);
            verify(residentService).createResident(request);
        }
    }

    @Nested
    @DisplayName("GET /{id} - Get Resident")
    class GetResident {

        @Test
        @DisplayName("Should return resident by ID with 200 OK")
        void shouldReturnResidentById() {
            ResidentResponse expectedResponse = ResidentResponse.builder()
                    .id(RESIDENT_ID)
                    .firstName("John")
                    .lastName("Doe")
                    .build();
            when(residentService.getResident(RESIDENT_ID)).thenReturn(expectedResponse);

            ResponseEntity<ApiResponse<ResidentResponse>> result = residentController.getResident(RESIDENT_ID);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).isEqualTo(expectedResponse);
            verify(residentService).getResident(RESIDENT_ID);
        }
    }

    @Nested
    @DisplayName("GET / - Get All Residents")
    class GetAllResidents {

        @Test
        @DisplayName("Should return all residents with pagination and 200 OK")
        void shouldReturnAllResidents() {
            PagedResponse<ResidentResponse> expectedResponse = PagedResponse.<ResidentResponse>builder()
                    .page(0)
                    .size(20)
                    .totalElements(0)
                    .totalPages(0)
                    .build();
            when(residentService.getAllResidents(PAGEABLE)).thenReturn(expectedResponse);

            ResponseEntity<ApiResponse<PagedResponse<ResidentResponse>>> result =
                    residentController.getAllResidents(PAGEABLE);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).isEqualTo(expectedResponse);
            verify(residentService).getAllResidents(PAGEABLE);
        }
    }

    @Nested
    @DisplayName("GET /search - Search Residents")
    class SearchResidents {

        @Test
        @DisplayName("Should search residents by query and return 200 OK")
        void shouldSearchResidents() {
            String query = "john";
            PagedResponse<ResidentResponse> expectedResponse = PagedResponse.<ResidentResponse>builder()
                    .page(0)
                    .size(20)
                    .totalElements(0)
                    .totalPages(0)
                    .build();
            when(residentService.searchResidents(query, PAGEABLE)).thenReturn(expectedResponse);

            ResponseEntity<ApiResponse<PagedResponse<ResidentResponse>>> result =
                    residentController.searchResidents(query, PAGEABLE);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).isEqualTo(expectedResponse);
            verify(residentService).searchResidents(query, PAGEABLE);
        }
    }

    @Nested
    @DisplayName("GET /me - Get My Profile")
    class GetMyProfile {

        @Test
        @DisplayName("Should return current user profile using JWT subject and 200 OK")
        void shouldReturnMyProfile() {
            String userId = "user-id-123";
            Jwt jwt = mock(Jwt.class);
            when(jwt.getSubject()).thenReturn(userId);
            ResidentResponse expectedResponse = ResidentResponse.builder()
                    .id(RESIDENT_ID)
                    .userId(userId)
                    .firstName("John")
                    .lastName("Doe")
                    .build();
            when(residentService.getByUserId(userId)).thenReturn(expectedResponse);

            ResponseEntity<ApiResponse<ResidentResponse>> result = residentController.getMyProfile(jwt);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).isEqualTo(expectedResponse);
            verify(jwt).getSubject();
            verify(residentService).getByUserId(userId);
        }
    }

    @Nested
    @DisplayName("PUT /{id} - Update Resident")
    class UpdateResident {

        @Test
        @DisplayName("Should update resident and return 200 OK")
        void shouldUpdateResident() {
            UpdateResidentRequest request = UpdateResidentRequest.builder()
                    .firstName("Jane")
                    .build();
            ResidentResponse expectedResponse = ResidentResponse.builder()
                    .id(RESIDENT_ID)
                    .firstName("Jane")
                    .lastName("Doe")
                    .build();
            when(residentService.updateResident(RESIDENT_ID, request)).thenReturn(expectedResponse);

            ResponseEntity<ApiResponse<ResidentResponse>> result =
                    residentController.updateResident(RESIDENT_ID, request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getMessage()).isEqualTo("Resident updated successfully");
            assertThat(result.getBody().getData()).isEqualTo(expectedResponse);
            verify(residentService).updateResident(RESIDENT_ID, request);
        }
    }

    @Nested
    @DisplayName("DELETE /{id} - Delete Resident")
    class DeleteResident {

        @Test
        @DisplayName("Should delete resident and return 200 OK")
        void shouldDeleteResident() {
            ResponseEntity<ApiResponse<Void>> result = residentController.deleteResident(RESIDENT_ID);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getMessage()).isEqualTo("Resident deleted successfully");
            assertThat(result.getBody().getData()).isNull();
            verify(residentService).deleteResident(RESIDENT_ID);
        }
    }

    @Nested
    @DisplayName("POST /{id}/link-user - Link Keycloak User")
    class LinkKeycloakUser {

        @Test
        @DisplayName("Should link Keycloak user and return 200 OK")
        void shouldLinkKeycloakUser() {
            LinkKeycloakUserRequest request = LinkKeycloakUserRequest.builder()
                    .userId("keycloak-user-456")
                    .build();
            ResidentResponse expectedResponse = ResidentResponse.builder()
                    .id(RESIDENT_ID)
                    .userId("keycloak-user-456")
                    .firstName("John")
                    .lastName("Doe")
                    .build();
            when(residentService.linkKeycloakUser(RESIDENT_ID, request)).thenReturn(expectedResponse);

            ResponseEntity<ApiResponse<ResidentResponse>> result =
                    residentController.linkKeycloakUser(RESIDENT_ID, request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getMessage()).isEqualTo("Keycloak user linked successfully");
            assertThat(result.getBody().getData()).isEqualTo(expectedResponse);
            verify(residentService).linkKeycloakUser(RESIDENT_ID, request);
        }
    }
}
