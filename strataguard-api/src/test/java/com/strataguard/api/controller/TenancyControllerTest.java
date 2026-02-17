package com.strataguard.api.controller;

import com.strataguard.core.dto.common.ApiResponse;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.dto.tenancy.CreateTenancyRequest;
import com.strataguard.core.dto.tenancy.TenancyResponse;
import com.strataguard.core.dto.tenancy.UpdateTenancyRequest;
import com.strataguard.service.resident.TenancyService;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenancyControllerTest {

    private static final UUID TENANCY_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID RESIDENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID UNIT_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final Pageable PAGEABLE = PageRequest.of(0, 20);

    @Mock
    private TenancyService tenancyService;

    @InjectMocks
    private TenancyController tenancyController;

    @Nested
    @DisplayName("POST / - Create Tenancy")
    class CreateTenancy {

        @Test
        @DisplayName("Should create tenancy and return 201 CREATED")
        void shouldCreateTenancyAndReturn201() {
            CreateTenancyRequest request = new CreateTenancyRequest();
            TenancyResponse expectedResponse = new TenancyResponse();
            when(tenancyService.createTenancy(request)).thenReturn(expectedResponse);

            ResponseEntity<ApiResponse<TenancyResponse>> result = tenancyController.createTenancy(request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getMessage()).isEqualTo("Tenancy created successfully");
            assertThat(result.getBody().getData()).isEqualTo(expectedResponse);
            verify(tenancyService).createTenancy(request);
        }
    }

    @Nested
    @DisplayName("GET /{id} - Get Tenancy")
    class GetTenancy {

        @Test
        @DisplayName("Should return tenancy by ID with 200 OK")
        void shouldReturnTenancyById() {
            TenancyResponse expectedResponse = new TenancyResponse();
            when(tenancyService.getTenancy(TENANCY_ID)).thenReturn(expectedResponse);

            ResponseEntity<ApiResponse<TenancyResponse>> result = tenancyController.getTenancy(TENANCY_ID);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).isEqualTo(expectedResponse);
            verify(tenancyService).getTenancy(TENANCY_ID);
        }
    }

    @Nested
    @DisplayName("GET /resident/{residentId} - Get Tenancies By Resident")
    class GetTenanciesByResident {

        @Test
        @DisplayName("Should return tenancies for a resident with 200 OK")
        void shouldReturnTenanciesByResident() {
            PagedResponse<TenancyResponse> expectedResponse = PagedResponse.<TenancyResponse>builder().build();
            when(tenancyService.getTenanciesByResident(RESIDENT_ID, PAGEABLE)).thenReturn(expectedResponse);

            ResponseEntity<ApiResponse<PagedResponse<TenancyResponse>>> result =
                    tenancyController.getTenanciesByResident(RESIDENT_ID, PAGEABLE);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).isEqualTo(expectedResponse);
            verify(tenancyService).getTenanciesByResident(RESIDENT_ID, PAGEABLE);
        }
    }

    @Nested
    @DisplayName("GET /unit/{unitId} - Get Tenancies By Unit")
    class GetTenanciesByUnit {

        @Test
        @DisplayName("Should return tenancies for a unit with 200 OK")
        void shouldReturnTenanciesByUnit() {
            PagedResponse<TenancyResponse> expectedResponse = PagedResponse.<TenancyResponse>builder().build();
            when(tenancyService.getTenanciesByUnit(UNIT_ID, PAGEABLE)).thenReturn(expectedResponse);

            ResponseEntity<ApiResponse<PagedResponse<TenancyResponse>>> result =
                    tenancyController.getTenanciesByUnit(UNIT_ID, PAGEABLE);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).isEqualTo(expectedResponse);
            verify(tenancyService).getTenanciesByUnit(UNIT_ID, PAGEABLE);
        }
    }

    @Nested
    @DisplayName("PUT /{id} - Update Tenancy")
    class UpdateTenancy {

        @Test
        @DisplayName("Should update tenancy and return 200 OK")
        void shouldUpdateTenancy() {
            UpdateTenancyRequest request = new UpdateTenancyRequest();
            TenancyResponse expectedResponse = new TenancyResponse();
            when(tenancyService.updateTenancy(TENANCY_ID, request)).thenReturn(expectedResponse);

            ResponseEntity<ApiResponse<TenancyResponse>> result =
                    tenancyController.updateTenancy(TENANCY_ID, request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getMessage()).isEqualTo("Tenancy updated successfully");
            assertThat(result.getBody().getData()).isEqualTo(expectedResponse);
            verify(tenancyService).updateTenancy(TENANCY_ID, request);
        }
    }

    @Nested
    @DisplayName("POST /{id}/terminate - Terminate Tenancy")
    class TerminateTenancy {

        @Test
        @DisplayName("Should terminate tenancy and return 200 OK")
        void shouldTerminateTenancy() {
            TenancyResponse expectedResponse = new TenancyResponse();
            when(tenancyService.terminateTenancy(TENANCY_ID)).thenReturn(expectedResponse);

            ResponseEntity<ApiResponse<TenancyResponse>> result = tenancyController.terminateTenancy(TENANCY_ID);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getMessage()).isEqualTo("Tenancy terminated successfully");
            assertThat(result.getBody().getData()).isEqualTo(expectedResponse);
            verify(tenancyService).terminateTenancy(TENANCY_ID);
        }
    }

    @Nested
    @DisplayName("DELETE /{id} - Delete Tenancy")
    class DeleteTenancy {

        @Test
        @DisplayName("Should delete tenancy and return 200 OK")
        void shouldDeleteTenancy() {
            ResponseEntity<ApiResponse<Void>> result = tenancyController.deleteTenancy(TENANCY_ID);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getMessage()).isEqualTo("Tenancy deleted successfully");
            assertThat(result.getBody().getData()).isNull();
            verify(tenancyService).deleteTenancy(TENANCY_ID);
        }
    }
}
