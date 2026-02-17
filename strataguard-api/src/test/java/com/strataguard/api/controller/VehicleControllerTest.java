package com.strataguard.api.controller;

import com.strataguard.core.dto.common.ApiResponse;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.dto.vehicle.BulkImportResponse;
import com.strataguard.core.dto.vehicle.CreateVehicleRequest;
import com.strataguard.core.dto.vehicle.UpdateVehicleRequest;
import com.strataguard.core.dto.vehicle.VehicleResponse;
import com.strataguard.service.resident.VehicleService;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleControllerTest {

    private static final UUID VEHICLE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID RESIDENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final Pageable PAGEABLE = PageRequest.of(0, 20);

    @Mock
    private VehicleService vehicleService;

    @InjectMocks
    private VehicleController vehicleController;

    @Nested
    @DisplayName("POST / - Create Vehicle")
    class CreateVehicle {

        @Test
        @DisplayName("Should create vehicle and return 201 CREATED")
        void shouldCreateVehicleAndReturn201() {
            CreateVehicleRequest request = new CreateVehicleRequest();
            VehicleResponse expectedResponse = new VehicleResponse();
            when(vehicleService.createVehicle(request)).thenReturn(expectedResponse);

            ResponseEntity<ApiResponse<VehicleResponse>> result = vehicleController.createVehicle(request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getMessage()).isEqualTo("Vehicle registered successfully");
            assertThat(result.getBody().getData()).isEqualTo(expectedResponse);
            verify(vehicleService).createVehicle(request);
        }
    }

    @Nested
    @DisplayName("GET /{id} - Get Vehicle")
    class GetVehicle {

        @Test
        @DisplayName("Should return vehicle by ID with 200 OK")
        void shouldReturnVehicleById() {
            VehicleResponse expectedResponse = new VehicleResponse();
            when(vehicleService.getVehicle(VEHICLE_ID)).thenReturn(expectedResponse);

            ResponseEntity<ApiResponse<VehicleResponse>> result = vehicleController.getVehicle(VEHICLE_ID);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).isEqualTo(expectedResponse);
            verify(vehicleService).getVehicle(VEHICLE_ID);
        }
    }

    @Nested
    @DisplayName("GET / - Get All Vehicles")
    class GetAllVehicles {

        @Test
        @DisplayName("Should return all vehicles with pagination and 200 OK")
        void shouldReturnAllVehicles() {
            PagedResponse<VehicleResponse> expectedResponse = PagedResponse.<VehicleResponse>builder().build();
            when(vehicleService.getAllVehicles(PAGEABLE)).thenReturn(expectedResponse);

            ResponseEntity<ApiResponse<PagedResponse<VehicleResponse>>> result =
                    vehicleController.getAllVehicles(PAGEABLE);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).isEqualTo(expectedResponse);
            verify(vehicleService).getAllVehicles(PAGEABLE);
        }
    }

    @Nested
    @DisplayName("GET /resident/{residentId} - Get Vehicles By Resident")
    class GetVehiclesByResident {

        @Test
        @DisplayName("Should return vehicles for a resident with 200 OK")
        void shouldReturnVehiclesByResident() {
            PagedResponse<VehicleResponse> expectedResponse = PagedResponse.<VehicleResponse>builder().build();
            when(vehicleService.getVehiclesByResident(RESIDENT_ID, PAGEABLE)).thenReturn(expectedResponse);

            ResponseEntity<ApiResponse<PagedResponse<VehicleResponse>>> result =
                    vehicleController.getVehiclesByResident(RESIDENT_ID, PAGEABLE);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).isEqualTo(expectedResponse);
            verify(vehicleService).getVehiclesByResident(RESIDENT_ID, PAGEABLE);
        }
    }

    @Nested
    @DisplayName("GET /search - Search Vehicles")
    class SearchVehicles {

        @Test
        @DisplayName("Should search vehicles by query and return 200 OK")
        void shouldSearchVehicles() {
            String query = "ABC-123";
            PagedResponse<VehicleResponse> expectedResponse = PagedResponse.<VehicleResponse>builder().build();
            when(vehicleService.searchVehicles(query, PAGEABLE)).thenReturn(expectedResponse);

            ResponseEntity<ApiResponse<PagedResponse<VehicleResponse>>> result =
                    vehicleController.searchVehicles(query, PAGEABLE);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).isEqualTo(expectedResponse);
            verify(vehicleService).searchVehicles(query, PAGEABLE);
        }
    }

    @Nested
    @DisplayName("PUT /{id} - Update Vehicle")
    class UpdateVehicle {

        @Test
        @DisplayName("Should update vehicle and return 200 OK")
        void shouldUpdateVehicle() {
            UpdateVehicleRequest request = new UpdateVehicleRequest();
            VehicleResponse expectedResponse = new VehicleResponse();
            when(vehicleService.updateVehicle(VEHICLE_ID, request)).thenReturn(expectedResponse);

            ResponseEntity<ApiResponse<VehicleResponse>> result =
                    vehicleController.updateVehicle(VEHICLE_ID, request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getMessage()).isEqualTo("Vehicle updated successfully");
            assertThat(result.getBody().getData()).isEqualTo(expectedResponse);
            verify(vehicleService).updateVehicle(VEHICLE_ID, request);
        }
    }

    @Nested
    @DisplayName("DELETE /{id} - Delete Vehicle")
    class DeleteVehicle {

        @Test
        @DisplayName("Should delete vehicle and return 200 OK")
        void shouldDeleteVehicle() {
            ResponseEntity<ApiResponse<Void>> result = vehicleController.deleteVehicle(VEHICLE_ID);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getMessage()).isEqualTo("Vehicle deleted successfully");
            assertThat(result.getBody().getData()).isNull();
            verify(vehicleService).deleteVehicle(VEHICLE_ID);
        }
    }

    @Nested
    @DisplayName("POST /import - Bulk Import Vehicles")
    class BulkImport {

        @Test
        @DisplayName("Should bulk import vehicles from file and return 200 OK")
        void shouldBulkImportVehicles() throws IOException {
            MultipartFile file = mock(MultipartFile.class);
            InputStream inputStream = new ByteArrayInputStream(new byte[0]);
            when(file.getInputStream()).thenReturn(inputStream);
            BulkImportResponse expectedResponse = BulkImportResponse.builder()
                    .totalRows(0)
                    .successCount(0)
                    .failureCount(0)
                    .build();
            when(vehicleService.bulkImport(any(InputStream.class))).thenReturn(expectedResponse);

            ResponseEntity<ApiResponse<BulkImportResponse>> result = vehicleController.bulkImport(file);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getMessage()).isEqualTo("Bulk import completed");
            assertThat(result.getBody().getData()).isEqualTo(expectedResponse);
            verify(file).getInputStream();
            verify(vehicleService).bulkImport(any(InputStream.class));
        }
    }
}
