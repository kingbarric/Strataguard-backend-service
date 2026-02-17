package com.strataguard.api.controller;

import com.strataguard.core.dto.billing.CreateLevyTypeRequest;
import com.strataguard.core.dto.billing.LevyTypeResponse;
import com.strataguard.core.dto.billing.UpdateLevyTypeRequest;
import com.strataguard.core.dto.common.ApiResponse;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.enums.LevyFrequency;
import com.strataguard.service.billing.LevyTypeService;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LevyTypeController")
class LevyTypeControllerTest {

    private static final UUID LEVY_TYPE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ESTATE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private LevyTypeService levyTypeService;

    @InjectMocks
    private LevyTypeController levyTypeController;

    private LevyTypeResponse buildLevyTypeResponse() {
        return LevyTypeResponse.builder()
                .id(LEVY_TYPE_ID)
                .name("Service Charge")
                .description("Monthly service charge")
                .amount(BigDecimal.valueOf(5000))
                .frequency(LevyFrequency.MONTHLY)
                .estateId(ESTATE_ID)
                .estateName("Sunrise Estate")
                .category("Maintenance")
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .createdBy("admin")
                .build();
    }

    private PagedResponse<LevyTypeResponse> buildPagedResponse() {
        return PagedResponse.<LevyTypeResponse>builder()
                .content(List.of(buildLevyTypeResponse()))
                .page(0)
                .size(20)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .build();
    }

    @Nested
    @DisplayName("POST / - Create Levy Type")
    class CreateLevyType {

        @Test
        @DisplayName("should return 201 CREATED with levy type data and success message")
        void shouldCreateLevyTypeSuccessfully() {
            CreateLevyTypeRequest request = CreateLevyTypeRequest.builder()
                    .name("Service Charge")
                    .description("Monthly service charge")
                    .amount(BigDecimal.valueOf(5000))
                    .frequency(LevyFrequency.MONTHLY)
                    .estateId(ESTATE_ID)
                    .category("Maintenance")
                    .build();

            LevyTypeResponse expectedResponse = buildLevyTypeResponse();
            when(levyTypeService.createLevyType(request)).thenReturn(expectedResponse);

            ResponseEntity<ApiResponse<LevyTypeResponse>> result = levyTypeController.createLevyType(request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getMessage()).isEqualTo("Levy type created successfully");
            assertThat(result.getBody().getData()).isEqualTo(expectedResponse);

            verify(levyTypeService).createLevyType(request);
        }
    }

    @Nested
    @DisplayName("GET / - Get All Levy Types")
    class GetAllLevyTypes {

        @Test
        @DisplayName("should return 200 OK with paged levy type data")
        void shouldGetAllLevyTypesSuccessfully() {
            Pageable pageable = PageRequest.of(0, 20);
            PagedResponse<LevyTypeResponse> pagedResponse = buildPagedResponse();

            when(levyTypeService.getAllLevyTypes(pageable)).thenReturn(pagedResponse);

            ResponseEntity<ApiResponse<PagedResponse<LevyTypeResponse>>> result =
                    levyTypeController.getAllLevyTypes(pageable);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).isEqualTo(pagedResponse);
            assertThat(result.getBody().getData().getContent()).hasSize(1);
            assertThat(result.getBody().getData().getTotalElements()).isEqualTo(1);

            verify(levyTypeService).getAllLevyTypes(pageable);
        }
    }

    @Nested
    @DisplayName("GET /{id} - Get Levy Type by ID")
    class GetLevyType {

        @Test
        @DisplayName("should return 200 OK with levy type data")
        void shouldGetLevyTypeSuccessfully() {
            LevyTypeResponse expectedResponse = buildLevyTypeResponse();
            when(levyTypeService.getLevyType(LEVY_TYPE_ID)).thenReturn(expectedResponse);

            ResponseEntity<ApiResponse<LevyTypeResponse>> result = levyTypeController.getLevyType(LEVY_TYPE_ID);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).isEqualTo(expectedResponse);

            verify(levyTypeService).getLevyType(LEVY_TYPE_ID);
        }
    }

    @Nested
    @DisplayName("PUT /{id} - Update Levy Type")
    class UpdateLevyType {

        @Test
        @DisplayName("should return 200 OK with updated levy type data and success message")
        void shouldUpdateLevyTypeSuccessfully() {
            UpdateLevyTypeRequest request = UpdateLevyTypeRequest.builder()
                    .name("Updated Service Charge")
                    .amount(BigDecimal.valueOf(6000))
                    .build();

            LevyTypeResponse expectedResponse = buildLevyTypeResponse();
            expectedResponse.setName("Updated Service Charge");
            expectedResponse.setAmount(BigDecimal.valueOf(6000));

            when(levyTypeService.updateLevyType(LEVY_TYPE_ID, request)).thenReturn(expectedResponse);

            ResponseEntity<ApiResponse<LevyTypeResponse>> result =
                    levyTypeController.updateLevyType(LEVY_TYPE_ID, request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getMessage()).isEqualTo("Levy type updated successfully");
            assertThat(result.getBody().getData()).isEqualTo(expectedResponse);

            verify(levyTypeService).updateLevyType(LEVY_TYPE_ID, request);
        }
    }

    @Nested
    @DisplayName("DELETE /{id} - Delete Levy Type")
    class DeleteLevyType {

        @Test
        @DisplayName("should return 200 OK with success message and null data")
        void shouldDeleteLevyTypeSuccessfully() {
            ResponseEntity<ApiResponse<Void>> result = levyTypeController.deleteLevyType(LEVY_TYPE_ID);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getMessage()).isEqualTo("Levy type deleted successfully");
            assertThat(result.getBody().getData()).isNull();

            verify(levyTypeService).deleteLevyType(LEVY_TYPE_ID);
        }
    }

    @Nested
    @DisplayName("GET /estate/{estateId} - Get Levy Types by Estate")
    class GetLevyTypesByEstate {

        @Test
        @DisplayName("should return 200 OK with paged levy type data for estate")
        void shouldGetLevyTypesByEstateSuccessfully() {
            Pageable pageable = PageRequest.of(0, 20);
            PagedResponse<LevyTypeResponse> pagedResponse = buildPagedResponse();

            when(levyTypeService.getLevyTypesByEstate(ESTATE_ID, pageable)).thenReturn(pagedResponse);

            ResponseEntity<ApiResponse<PagedResponse<LevyTypeResponse>>> result =
                    levyTypeController.getLevyTypesByEstate(ESTATE_ID, pageable);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).isEqualTo(pagedResponse);
            assertThat(result.getBody().getData().getContent()).hasSize(1);
            assertThat(result.getBody().getData().getTotalElements()).isEqualTo(1);

            verify(levyTypeService).getLevyTypesByEstate(ESTATE_ID, pageable);
        }
    }
}
