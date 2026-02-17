package com.strataguard.api.controller;

import com.strataguard.core.dto.common.ApiResponse;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.dto.unit.CreateUnitRequest;
import com.strataguard.core.dto.unit.UnitResponse;
import com.strataguard.core.dto.unit.UpdateUnitRequest;
import com.strataguard.core.enums.UnitStatus;
import com.strataguard.core.enums.UnitType;
import com.strataguard.service.estate.UnitService;
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
@DisplayName("UnitController")
class UnitControllerTest {

    private static final UUID UNIT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ESTATE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private UnitService unitService;

    @InjectMocks
    private UnitController unitController;

    private UnitResponse buildUnitResponse() {
        return UnitResponse.builder()
                .id(UNIT_ID)
                .estateId(ESTATE_ID)
                .unitNumber("A-101")
                .blockOrZone("Block A")
                .unitType(UnitType.FLAT)
                .floor(1)
                .status(UnitStatus.VACANT)
                .bedrooms(3)
                .bathrooms(2)
                .squareMeters(85.0)
                .description("Spacious 3-bedroom apartment")
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("POST / - Create Unit")
    class CreateUnit {

        @Test
        @DisplayName("should return 201 CREATED with unit data and success message")
        void shouldCreateUnitSuccessfully() {
            CreateUnitRequest request = CreateUnitRequest.builder()
                    .estateId(ESTATE_ID)
                    .unitNumber("A-101")
                    .blockOrZone("Block A")
                    .unitType(UnitType.FLAT)
                    .floor(1)
                    .bedrooms(3)
                    .bathrooms(2)
                    .squareMeters(85.0)
                    .description("Spacious 3-bedroom apartment")
                    .build();

            UnitResponse expectedResponse = buildUnitResponse();
            when(unitService.createUnit(request)).thenReturn(expectedResponse);

            ResponseEntity<ApiResponse<UnitResponse>> result = unitController.createUnit(request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getMessage()).isEqualTo("Unit created successfully");
            assertThat(result.getBody().getData()).isEqualTo(expectedResponse);

            verify(unitService).createUnit(request);
        }
    }

    @Nested
    @DisplayName("GET /{id} - Get Unit by ID")
    class GetUnit {

        @Test
        @DisplayName("should return 200 OK with unit data")
        void shouldGetUnitSuccessfully() {
            UnitResponse expectedResponse = buildUnitResponse();
            when(unitService.getUnit(UNIT_ID)).thenReturn(expectedResponse);

            ResponseEntity<ApiResponse<UnitResponse>> result = unitController.getUnit(UNIT_ID);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).isEqualTo(expectedResponse);

            verify(unitService).getUnit(UNIT_ID);
        }
    }

    @Nested
    @DisplayName("GET /estate/{estateId} - Get Units by Estate")
    class GetUnitsByEstate {

        @Test
        @DisplayName("should return 200 OK with paged unit data")
        void shouldGetUnitsByEstateSuccessfully() {
            Pageable pageable = PageRequest.of(0, 20);
            PagedResponse<UnitResponse> pagedResponse = PagedResponse.<UnitResponse>builder()
                    .content(List.of(buildUnitResponse()))
                    .page(0)
                    .size(20)
                    .totalElements(1)
                    .totalPages(1)
                    .first(true)
                    .last(true)
                    .build();

            when(unitService.getUnitsByEstate(ESTATE_ID, pageable)).thenReturn(pagedResponse);

            ResponseEntity<ApiResponse<PagedResponse<UnitResponse>>> result =
                    unitController.getUnitsByEstate(ESTATE_ID, pageable);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).isEqualTo(pagedResponse);
            assertThat(result.getBody().getData().getContent()).hasSize(1);
            assertThat(result.getBody().getData().getTotalElements()).isEqualTo(1);

            verify(unitService).getUnitsByEstate(ESTATE_ID, pageable);
        }
    }

    @Nested
    @DisplayName("PUT /{id} - Update Unit")
    class UpdateUnit {

        @Test
        @DisplayName("should return 200 OK with updated unit data and success message")
        void shouldUpdateUnitSuccessfully() {
            UpdateUnitRequest request = UpdateUnitRequest.builder()
                    .unitNumber("A-102")
                    .bedrooms(4)
                    .squareMeters(100.0)
                    .build();

            UnitResponse expectedResponse = buildUnitResponse();
            expectedResponse.setUnitNumber("A-102");
            when(unitService.updateUnit(UNIT_ID, request)).thenReturn(expectedResponse);

            ResponseEntity<ApiResponse<UnitResponse>> result =
                    unitController.updateUnit(UNIT_ID, request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getMessage()).isEqualTo("Unit updated successfully");
            assertThat(result.getBody().getData()).isEqualTo(expectedResponse);

            verify(unitService).updateUnit(UNIT_ID, request);
        }
    }

    @Nested
    @DisplayName("DELETE /{id} - Delete Unit")
    class DeleteUnit {

        @Test
        @DisplayName("should return 200 OK with success message and null data")
        void shouldDeleteUnitSuccessfully() {
            ResponseEntity<ApiResponse<Void>> result = unitController.deleteUnit(UNIT_ID);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getMessage()).isEqualTo("Unit deleted successfully");
            assertThat(result.getBody().getData()).isNull();

            verify(unitService).deleteUnit(UNIT_ID);
        }
    }

    @Nested
    @DisplayName("GET /estate/{estateId}/count - Count Units by Estate")
    class CountUnitsByEstate {

        @Test
        @DisplayName("should return 200 OK with unit count")
        void shouldCountUnitsByEstateSuccessfully() {
            long expectedCount = 42L;
            when(unitService.countUnitsByEstate(ESTATE_ID)).thenReturn(expectedCount);

            ResponseEntity<ApiResponse<Long>> result = unitController.countUnits(ESTATE_ID);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).isEqualTo(42L);

            verify(unitService).countUnitsByEstate(ESTATE_ID);
        }
    }
}
