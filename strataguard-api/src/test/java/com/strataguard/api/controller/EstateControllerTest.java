package com.strataguard.api.controller;

import com.strataguard.core.dto.common.ApiResponse;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.dto.estate.CreateEstateRequest;
import com.strataguard.core.dto.estate.EstateResponse;
import com.strataguard.core.dto.estate.UpdateEstateRequest;
import com.strataguard.core.enums.EstateType;
import com.strataguard.service.estate.EstateService;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EstateController")
class EstateControllerTest {

    private static final UUID ESTATE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private EstateService estateService;

    @InjectMocks
    private EstateController estateController;

    private EstateResponse buildEstateResponse() {
        return EstateResponse.builder()
                .id(ESTATE_ID)
                .name("Sunrise Estate")
                .address("123 Main Street")
                .city("Nairobi")
                .state("Nairobi County")
                .country("Kenya")
                .estateType(EstateType.RESIDENTIAL_ESTATE)
                .description("A beautiful residential estate")
                .contactEmail("info@sunrise.co.ke")
                .contactPhone("+254700000000")
                .totalUnits(50)
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("POST / - Create Estate")
    class CreateEstate {

        @Test
        @DisplayName("should return 201 CREATED with estate data and success message")
        void shouldCreateEstateSuccessfully() {
            CreateEstateRequest request = CreateEstateRequest.builder()
                    .name("Sunrise Estate")
                    .address("123 Main Street")
                    .city("Nairobi")
                    .estateType(EstateType.RESIDENTIAL_ESTATE)
                    .contactEmail("info@sunrise.co.ke")
                    .totalUnits(50)
                    .build();

            EstateResponse expectedResponse = buildEstateResponse();
            when(estateService.createEstate(request)).thenReturn(expectedResponse);

            ResponseEntity<ApiResponse<EstateResponse>> result = estateController.createEstate(request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getMessage()).isEqualTo("Estate created successfully");
            assertThat(result.getBody().getData()).isEqualTo(expectedResponse);

            verify(estateService).createEstate(request);
        }
    }

    @Nested
    @DisplayName("GET /{id} - Get Estate by ID")
    class GetEstate {

        @Test
        @DisplayName("should return 200 OK with estate data")
        void shouldGetEstateSuccessfully() {
            EstateResponse expectedResponse = buildEstateResponse();
            when(estateService.getEstate(ESTATE_ID)).thenReturn(expectedResponse);

            ResponseEntity<ApiResponse<EstateResponse>> result = estateController.getEstate(ESTATE_ID);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).isEqualTo(expectedResponse);

            verify(estateService).getEstate(ESTATE_ID);
        }
    }

    @Nested
    @DisplayName("GET / - Get All Estates")
    class GetAllEstates {

        @Test
        @DisplayName("should return 200 OK with paged estate data")
        void shouldGetAllEstatesSuccessfully() {
            Pageable pageable = PageRequest.of(0, 20);
            PagedResponse<EstateResponse> pagedResponse = PagedResponse.<EstateResponse>builder()
                    .content(List.of(buildEstateResponse()))
                    .page(0)
                    .size(20)
                    .totalElements(1)
                    .totalPages(1)
                    .first(true)
                    .last(true)
                    .build();

            when(estateService.getAllEstates(pageable)).thenReturn(pagedResponse);

            ResponseEntity<ApiResponse<PagedResponse<EstateResponse>>> result =
                    estateController.getAllEstates(pageable);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).isEqualTo(pagedResponse);
            assertThat(result.getBody().getData().getContent()).hasSize(1);
            assertThat(result.getBody().getData().getTotalElements()).isEqualTo(1);

            verify(estateService).getAllEstates(pageable);
        }
    }

    @Nested
    @DisplayName("GET /search - Search Estates")
    class SearchEstates {

        @Test
        @DisplayName("should return 200 OK with search results")
        void shouldSearchEstatesSuccessfully() {
            String query = "Sunrise";
            Pageable pageable = PageRequest.of(0, 20);
            PagedResponse<EstateResponse> pagedResponse = PagedResponse.<EstateResponse>builder()
                    .content(List.of(buildEstateResponse()))
                    .page(0)
                    .size(20)
                    .totalElements(1)
                    .totalPages(1)
                    .first(true)
                    .last(true)
                    .build();

            when(estateService.searchEstates(query, pageable)).thenReturn(pagedResponse);

            ResponseEntity<ApiResponse<PagedResponse<EstateResponse>>> result =
                    estateController.searchEstates(query, pageable);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).isEqualTo(pagedResponse);
            assertThat(result.getBody().getData().getContent()).hasSize(1);

            verify(estateService).searchEstates(query, pageable);
        }
    }

    @Nested
    @DisplayName("PUT /{id} - Update Estate")
    class UpdateEstate {

        @Test
        @DisplayName("should return 200 OK with updated estate data and success message")
        void shouldUpdateEstateSuccessfully() {
            UpdateEstateRequest request = UpdateEstateRequest.builder()
                    .name("Sunrise Estate Updated")
                    .address("456 New Avenue")
                    .city("Mombasa")
                    .build();

            EstateResponse expectedResponse = buildEstateResponse();
            expectedResponse.setName("Sunrise Estate Updated");
            when(estateService.updateEstate(ESTATE_ID, request)).thenReturn(expectedResponse);

            ResponseEntity<ApiResponse<EstateResponse>> result =
                    estateController.updateEstate(ESTATE_ID, request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getMessage()).isEqualTo("Estate updated successfully");
            assertThat(result.getBody().getData()).isEqualTo(expectedResponse);

            verify(estateService).updateEstate(ESTATE_ID, request);
        }
    }

    @Nested
    @DisplayName("DELETE /{id} - Delete Estate")
    class DeleteEstate {

        @Test
        @DisplayName("should return 200 OK with success message and null data")
        void shouldDeleteEstateSuccessfully() {
            ResponseEntity<ApiResponse<Void>> result = estateController.deleteEstate(ESTATE_ID);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getMessage()).isEqualTo("Estate deleted successfully");
            assertThat(result.getBody().getData()).isNull();

            verify(estateService).deleteEstate(ESTATE_ID);
        }
    }
}
