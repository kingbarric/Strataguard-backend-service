package com.strataguard.service.resident;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.dto.vehicle.CreateVehicleRequest;
import com.strataguard.core.dto.vehicle.UpdateVehicleRequest;
import com.strataguard.core.dto.vehicle.VehicleResponse;
import com.strataguard.core.entity.Resident;
import com.strataguard.core.entity.Vehicle;
import com.strataguard.core.enums.VehicleStatus;
import com.strataguard.core.enums.VehicleType;
import com.strataguard.core.exception.DuplicateResourceException;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.core.util.VehicleMapper;
import com.strataguard.infrastructure.repository.ResidentRepository;
import com.strataguard.infrastructure.repository.VehicleRepository;
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
class VehicleServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private ResidentRepository residentRepository;

    @Mock
    private VehicleMapper vehicleMapper;

    @InjectMocks
    private VehicleService vehicleService;

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID VEHICLE_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID RESIDENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");

    private Vehicle vehicle;
    private Resident resident;
    private VehicleResponse vehicleResponse;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID);

        resident = new Resident();
        resident.setId(RESIDENT_ID);
        resident.setTenantId(TENANT_ID);
        resident.setFirstName("John");
        resident.setLastName("Doe");

        vehicle = new Vehicle();
        vehicle.setId(VEHICLE_ID);
        vehicle.setTenantId(TENANT_ID);
        vehicle.setResidentId(RESIDENT_ID);
        vehicle.setPlateNumber("ABC123");
        vehicle.setMake("Toyota");
        vehicle.setModel("Corolla");
        vehicle.setColor("White");
        vehicle.setVehicleType(VehicleType.CAR);
        vehicle.setQrStickerCode("VEH-some-uuid");
        vehicle.setStickerNumber("STK-001");
        vehicle.setStatus(VehicleStatus.ACTIVE);
        vehicle.setActive(true);
        vehicle.setDeleted(false);
        vehicle.setCreatedAt(Instant.now());
        vehicle.setUpdatedAt(Instant.now());

        vehicleResponse = VehicleResponse.builder()
                .id(VEHICLE_ID)
                .residentId(RESIDENT_ID)
                .plateNumber("ABC123")
                .make("Toyota")
                .model("Corolla")
                .color("White")
                .vehicleType(VehicleType.CAR)
                .qrStickerCode("VEH-some-uuid")
                .stickerNumber("STK-001")
                .status(VehicleStatus.ACTIVE)
                .active(true)
                .createdAt(vehicle.getCreatedAt())
                .updatedAt(vehicle.getUpdatedAt())
                .build();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ========================================================================
    // createVehicle
    // ========================================================================

    @Nested
    @DisplayName("createVehicle")
    class CreateVehicle {

        private CreateVehicleRequest createRequest;

        @BeforeEach
        void setUp() {
            createRequest = CreateVehicleRequest.builder()
                    .residentId(RESIDENT_ID)
                    .plateNumber("abc-123")
                    .make("Toyota")
                    .model("Corolla")
                    .color("White")
                    .vehicleType(VehicleType.CAR)
                    .stickerNumber("STK-001")
                    .build();
        }

        @Test
        @DisplayName("should create vehicle successfully with normalized plate and generated QR sticker code")
        void shouldCreateVehicleSuccessfully() {
            when(residentRepository.findByIdAndTenantId(RESIDENT_ID, TENANT_ID))
                    .thenReturn(Optional.of(resident));
            when(vehicleRepository.existsByPlateNumberAndTenantId("ABC123", TENANT_ID))
                    .thenReturn(false);
            when(vehicleMapper.toEntity(createRequest)).thenReturn(vehicle);
            when(vehicleRepository.save(vehicle)).thenReturn(vehicle);
            when(vehicleMapper.toResponse(vehicle)).thenReturn(vehicleResponse);

            VehicleResponse result = vehicleService.createVehicle(createRequest);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(VEHICLE_ID);
            assertThat(result.getResidentId()).isEqualTo(RESIDENT_ID);
            assertThat(result.getPlateNumber()).isEqualTo("ABC123");
            assertThat(result.getMake()).isEqualTo("Toyota");
            assertThat(result.getModel()).isEqualTo("Corolla");
            assertThat(result.getVehicleType()).isEqualTo(VehicleType.CAR);
            assertThat(result.isActive()).isTrue();

            verify(residentRepository).findByIdAndTenantId(RESIDENT_ID, TENANT_ID);
            verify(vehicleRepository).existsByPlateNumberAndTenantId("ABC123", TENANT_ID);
            verify(vehicleMapper).toEntity(createRequest);
            verify(vehicleRepository).save(vehicle);
            verify(vehicleMapper).toResponse(vehicle);
        }

        @Test
        @DisplayName("should set tenant ID and normalized plate number on created vehicle")
        void shouldSetTenantIdAndNormalizedPlate() {
            Vehicle newVehicle = new Vehicle();
            when(residentRepository.findByIdAndTenantId(RESIDENT_ID, TENANT_ID))
                    .thenReturn(Optional.of(resident));
            when(vehicleRepository.existsByPlateNumberAndTenantId("ABC123", TENANT_ID))
                    .thenReturn(false);
            when(vehicleMapper.toEntity(createRequest)).thenReturn(newVehicle);
            when(vehicleRepository.save(newVehicle)).thenReturn(newVehicle);
            when(vehicleMapper.toResponse(newVehicle)).thenReturn(vehicleResponse);

            vehicleService.createVehicle(createRequest);

            assertThat(newVehicle.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(newVehicle.getPlateNumber()).isEqualTo("ABC123");
            assertThat(newVehicle.getQrStickerCode()).startsWith("VEH-");
            verify(vehicleRepository).save(newVehicle);
        }

        @Test
        @DisplayName("should generate QR sticker code starting with VEH- prefix")
        void shouldGenerateQrStickerCode() {
            Vehicle newVehicle = new Vehicle();
            when(residentRepository.findByIdAndTenantId(RESIDENT_ID, TENANT_ID))
                    .thenReturn(Optional.of(resident));
            when(vehicleRepository.existsByPlateNumberAndTenantId("ABC123", TENANT_ID))
                    .thenReturn(false);
            when(vehicleMapper.toEntity(createRequest)).thenReturn(newVehicle);
            when(vehicleRepository.save(newVehicle)).thenReturn(newVehicle);
            when(vehicleMapper.toResponse(newVehicle)).thenReturn(vehicleResponse);

            vehicleService.createVehicle(createRequest);

            assertThat(newVehicle.getQrStickerCode()).isNotNull();
            assertThat(newVehicle.getQrStickerCode()).startsWith("VEH-");
            assertThat(newVehicle.getQrStickerCode()).hasSizeGreaterThan(4);
        }

        @Test
        @DisplayName("should normalize plate number by uppercasing and removing spaces and dashes")
        void shouldNormalizePlateNumber() {
            CreateVehicleRequest requestWithSpaces = CreateVehicleRequest.builder()
                    .residentId(RESIDENT_ID)
                    .plateNumber("ab c-1 23")
                    .make("Toyota")
                    .model("Corolla")
                    .color("White")
                    .vehicleType(VehicleType.CAR)
                    .build();

            when(residentRepository.findByIdAndTenantId(RESIDENT_ID, TENANT_ID))
                    .thenReturn(Optional.of(resident));
            when(vehicleRepository.existsByPlateNumberAndTenantId("ABC123", TENANT_ID))
                    .thenReturn(false);
            when(vehicleMapper.toEntity(requestWithSpaces)).thenReturn(vehicle);
            when(vehicleRepository.save(vehicle)).thenReturn(vehicle);
            when(vehicleMapper.toResponse(vehicle)).thenReturn(vehicleResponse);

            vehicleService.createVehicle(requestWithSpaces);

            assertThat(vehicle.getPlateNumber()).isEqualTo("ABC123");
            verify(vehicleRepository).existsByPlateNumberAndTenantId("ABC123", TENANT_ID);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when resident does not exist")
        void shouldThrowResourceNotFoundExceptionWhenResidentNotFound() {
            UUID nonExistentResidentId = UUID.fromString("00000000-0000-0000-0000-000000000099");
            CreateVehicleRequest requestWithBadResident = CreateVehicleRequest.builder()
                    .residentId(nonExistentResidentId)
                    .plateNumber("XYZ789")
                    .make("Honda")
                    .model("Civic")
                    .vehicleType(VehicleType.CAR)
                    .build();

            when(residentRepository.findByIdAndTenantId(nonExistentResidentId, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> vehicleService.createVehicle(requestWithBadResident))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Resident")
                    .hasMessageContaining("id");

            verify(residentRepository).findByIdAndTenantId(nonExistentResidentId, TENANT_ID);
            verify(vehicleRepository, never()).existsByPlateNumberAndTenantId(any(), any());
            verify(vehicleRepository, never()).save(any(Vehicle.class));
        }

        @Test
        @DisplayName("should throw DuplicateResourceException when plate number already exists for tenant")
        void shouldThrowDuplicateResourceExceptionWhenPlateExists() {
            when(residentRepository.findByIdAndTenantId(RESIDENT_ID, TENANT_ID))
                    .thenReturn(Optional.of(resident));
            when(vehicleRepository.existsByPlateNumberAndTenantId("ABC123", TENANT_ID))
                    .thenReturn(true);

            assertThatThrownBy(() -> vehicleService.createVehicle(createRequest))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("Vehicle")
                    .hasMessageContaining("plateNumber")
                    .hasMessageContaining("ABC123");

            verify(residentRepository).findByIdAndTenantId(RESIDENT_ID, TENANT_ID);
            verify(vehicleRepository).existsByPlateNumberAndTenantId("ABC123", TENANT_ID);
            verify(vehicleRepository, never()).save(any(Vehicle.class));
            verify(vehicleMapper, never()).toEntity(any(CreateVehicleRequest.class));
        }
    }

    // ========================================================================
    // getVehicle
    // ========================================================================

    @Nested
    @DisplayName("getVehicle")
    class GetVehicle {

        @Test
        @DisplayName("should return vehicle when found by id and tenant")
        void shouldReturnVehicleWhenFound() {
            when(vehicleRepository.findByIdAndTenantId(VEHICLE_ID, TENANT_ID))
                    .thenReturn(Optional.of(vehicle));
            when(vehicleMapper.toResponse(vehicle)).thenReturn(vehicleResponse);

            VehicleResponse result = vehicleService.getVehicle(VEHICLE_ID);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(VEHICLE_ID);
            assertThat(result.getPlateNumber()).isEqualTo("ABC123");
            assertThat(result.getMake()).isEqualTo("Toyota");
            assertThat(result.getModel()).isEqualTo("Corolla");
            assertThat(result.getVehicleType()).isEqualTo(VehicleType.CAR);
            assertThat(result.getQrStickerCode()).isEqualTo("VEH-some-uuid");
            assertThat(result.getStatus()).isEqualTo(VehicleStatus.ACTIVE);

            verify(vehicleRepository).findByIdAndTenantId(VEHICLE_ID, TENANT_ID);
            verify(vehicleMapper).toResponse(vehicle);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when vehicle not found")
        void shouldThrowResourceNotFoundExceptionWhenNotFound() {
            UUID nonExistentId = UUID.fromString("00000000-0000-0000-0000-000000000099");
            when(vehicleRepository.findByIdAndTenantId(nonExistentId, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> vehicleService.getVehicle(nonExistentId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Vehicle")
                    .hasMessageContaining("id");

            verify(vehicleRepository).findByIdAndTenantId(nonExistentId, TENANT_ID);
            verify(vehicleMapper, never()).toResponse(any(Vehicle.class));
        }
    }

    // ========================================================================
    // getAllVehicles
    // ========================================================================

    @Nested
    @DisplayName("getAllVehicles")
    class GetAllVehicles {

        @Test
        @DisplayName("should return paginated list of vehicles for tenant")
        void shouldReturnPaginatedVehicles() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Vehicle> page = new PageImpl<>(List.of(vehicle), pageable, 1);

            when(vehicleRepository.findAllByTenantId(TENANT_ID, pageable)).thenReturn(page);
            when(vehicleMapper.toResponse(vehicle)).thenReturn(vehicleResponse);

            PagedResponse<VehicleResponse> result = vehicleService.getAllVehicles(pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getId()).isEqualTo(VEHICLE_ID);
            assertThat(result.getContent().get(0).getPlateNumber()).isEqualTo("ABC123");
            assertThat(result.getPage()).isZero();
            assertThat(result.getSize()).isEqualTo(10);
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getTotalPages()).isEqualTo(1);
            assertThat(result.isFirst()).isTrue();
            assertThat(result.isLast()).isTrue();

            verify(vehicleRepository).findAllByTenantId(TENANT_ID, pageable);
        }

        @Test
        @DisplayName("should return empty page when no vehicles exist for tenant")
        void shouldReturnEmptyPageWhenNoVehicles() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Vehicle> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(vehicleRepository.findAllByTenantId(TENANT_ID, pageable)).thenReturn(emptyPage);

            PagedResponse<VehicleResponse> result = vehicleService.getAllVehicles(pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("should pass correct pageable parameters to repository")
        void shouldPassCorrectPageableToRepository() {
            Pageable pageable = PageRequest.of(2, 5);
            Page<Vehicle> page = new PageImpl<>(List.of(), pageable, 0);

            when(vehicleRepository.findAllByTenantId(TENANT_ID, pageable)).thenReturn(page);

            vehicleService.getAllVehicles(pageable);

            verify(vehicleRepository).findAllByTenantId(TENANT_ID, pageable);
        }
    }

    // ========================================================================
    // getVehiclesByResident
    // ========================================================================

    @Nested
    @DisplayName("getVehiclesByResident")
    class GetVehiclesByResident {

        @Test
        @DisplayName("should return paginated vehicles for a specific resident")
        void shouldReturnPaginatedVehiclesForResident() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Vehicle> page = new PageImpl<>(List.of(vehicle), pageable, 1);

            when(residentRepository.findByIdAndTenantId(RESIDENT_ID, TENANT_ID))
                    .thenReturn(Optional.of(resident));
            when(vehicleRepository.findByResidentIdAndTenantId(RESIDENT_ID, TENANT_ID, pageable))
                    .thenReturn(page);
            when(vehicleMapper.toResponse(vehicle)).thenReturn(vehicleResponse);

            PagedResponse<VehicleResponse> result = vehicleService.getVehiclesByResident(RESIDENT_ID, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getResidentId()).isEqualTo(RESIDENT_ID);
            assertThat(result.getContent().get(0).getPlateNumber()).isEqualTo("ABC123");
            assertThat(result.getPage()).isZero();
            assertThat(result.getSize()).isEqualTo(10);
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getTotalPages()).isEqualTo(1);
            assertThat(result.isFirst()).isTrue();
            assertThat(result.isLast()).isTrue();

            verify(residentRepository).findByIdAndTenantId(RESIDENT_ID, TENANT_ID);
            verify(vehicleRepository).findByResidentIdAndTenantId(RESIDENT_ID, TENANT_ID, pageable);
        }

        @Test
        @DisplayName("should return empty page when resident has no vehicles")
        void shouldReturnEmptyPageWhenResidentHasNoVehicles() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Vehicle> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(residentRepository.findByIdAndTenantId(RESIDENT_ID, TENANT_ID))
                    .thenReturn(Optional.of(resident));
            when(vehicleRepository.findByResidentIdAndTenantId(RESIDENT_ID, TENANT_ID, pageable))
                    .thenReturn(emptyPage);

            PagedResponse<VehicleResponse> result = vehicleService.getVehiclesByResident(RESIDENT_ID, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();

            verify(residentRepository).findByIdAndTenantId(RESIDENT_ID, TENANT_ID);
            verify(vehicleRepository).findByResidentIdAndTenantId(RESIDENT_ID, TENANT_ID, pageable);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when resident does not exist")
        void shouldThrowResourceNotFoundExceptionWhenResidentNotFound() {
            UUID nonExistentResidentId = UUID.fromString("00000000-0000-0000-0000-000000000099");
            Pageable pageable = PageRequest.of(0, 10);

            when(residentRepository.findByIdAndTenantId(nonExistentResidentId, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> vehicleService.getVehiclesByResident(nonExistentResidentId, pageable))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Resident")
                    .hasMessageContaining("id");

            verify(residentRepository).findByIdAndTenantId(nonExistentResidentId, TENANT_ID);
            verify(vehicleRepository, never()).findByResidentIdAndTenantId(any(), any(), any());
        }
    }

    // ========================================================================
    // searchVehicles
    // ========================================================================

    @Nested
    @DisplayName("searchVehicles")
    class SearchVehicles {

        @Test
        @DisplayName("should return matching vehicles for search term")
        void shouldReturnMatchingVehicles() {
            String search = "Toyota";
            Pageable pageable = PageRequest.of(0, 10);
            Page<Vehicle> page = new PageImpl<>(List.of(vehicle), pageable, 1);

            when(vehicleRepository.searchByTenantId(TENANT_ID, search, pageable)).thenReturn(page);
            when(vehicleMapper.toResponse(vehicle)).thenReturn(vehicleResponse);

            PagedResponse<VehicleResponse> result = vehicleService.searchVehicles(search, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getMake()).isEqualTo("Toyota");
            assertThat(result.getContent().get(0).getPlateNumber()).isEqualTo("ABC123");

            verify(vehicleRepository).searchByTenantId(TENANT_ID, search, pageable);
        }

        @Test
        @DisplayName("should return matching vehicles when searching by plate number")
        void shouldReturnMatchingVehiclesWhenSearchingByPlate() {
            String search = "ABC";
            Pageable pageable = PageRequest.of(0, 10);
            Page<Vehicle> page = new PageImpl<>(List.of(vehicle), pageable, 1);

            when(vehicleRepository.searchByTenantId(TENANT_ID, search, pageable)).thenReturn(page);
            when(vehicleMapper.toResponse(vehicle)).thenReturn(vehicleResponse);

            PagedResponse<VehicleResponse> result = vehicleService.searchVehicles(search, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);

            verify(vehicleRepository).searchByTenantId(TENANT_ID, search, pageable);
        }

        @Test
        @DisplayName("should return empty page when no vehicles match search")
        void shouldReturnEmptyPageWhenNoMatch() {
            String search = "NonExistentBrand";
            Pageable pageable = PageRequest.of(0, 10);
            Page<Vehicle> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(vehicleRepository.searchByTenantId(TENANT_ID, search, pageable)).thenReturn(emptyPage);

            PagedResponse<VehicleResponse> result = vehicleService.searchVehicles(search, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();

            verify(vehicleRepository).searchByTenantId(TENANT_ID, search, pageable);
        }
    }

    // ========================================================================
    // updateVehicle
    // ========================================================================

    @Nested
    @DisplayName("updateVehicle")
    class UpdateVehicle {

        private UpdateVehicleRequest updateRequest;

        @BeforeEach
        void setUp() {
            updateRequest = UpdateVehicleRequest.builder()
                    .plateNumber("xyz-789")
                    .make("Honda")
                    .model("Civic")
                    .color("Black")
                    .vehicleType(VehicleType.SUV)
                    .stickerNumber("STK-002")
                    .build();
        }

        @Test
        @DisplayName("should update vehicle successfully when found")
        void shouldUpdateVehicleSuccessfully() {
            Vehicle updatedVehicle = new Vehicle();
            updatedVehicle.setId(VEHICLE_ID);
            updatedVehicle.setTenantId(TENANT_ID);
            updatedVehicle.setResidentId(RESIDENT_ID);
            updatedVehicle.setPlateNumber("XYZ789");
            updatedVehicle.setMake("Honda");
            updatedVehicle.setModel("Civic");
            updatedVehicle.setColor("Black");
            updatedVehicle.setVehicleType(VehicleType.SUV);
            updatedVehicle.setStickerNumber("STK-002");
            updatedVehicle.setStatus(VehicleStatus.ACTIVE);
            updatedVehicle.setActive(true);

            VehicleResponse updatedResponse = VehicleResponse.builder()
                    .id(VEHICLE_ID)
                    .residentId(RESIDENT_ID)
                    .plateNumber("XYZ789")
                    .make("Honda")
                    .model("Civic")
                    .color("Black")
                    .vehicleType(VehicleType.SUV)
                    .stickerNumber("STK-002")
                    .status(VehicleStatus.ACTIVE)
                    .active(true)
                    .build();

            when(vehicleRepository.findByIdAndTenantId(VEHICLE_ID, TENANT_ID))
                    .thenReturn(Optional.of(vehicle));
            when(vehicleRepository.existsByPlateNumberAndTenantId("XYZ789", TENANT_ID))
                    .thenReturn(false);
            when(vehicleRepository.save(vehicle)).thenReturn(updatedVehicle);
            when(vehicleMapper.toResponse(updatedVehicle)).thenReturn(updatedResponse);

            VehicleResponse result = vehicleService.updateVehicle(VEHICLE_ID, updateRequest);

            assertThat(result).isNotNull();
            assertThat(result.getPlateNumber()).isEqualTo("XYZ789");
            assertThat(result.getMake()).isEqualTo("Honda");
            assertThat(result.getModel()).isEqualTo("Civic");
            assertThat(result.getColor()).isEqualTo("Black");
            assertThat(result.getVehicleType()).isEqualTo(VehicleType.SUV);

            verify(vehicleRepository).findByIdAndTenantId(VEHICLE_ID, TENANT_ID);
            verify(vehicleRepository).existsByPlateNumberAndTenantId("XYZ789", TENANT_ID);
            verify(vehicleMapper).updateEntity(updateRequest, vehicle);
            verify(vehicleRepository).save(vehicle);
            verify(vehicleMapper).toResponse(updatedVehicle);
        }

        @Test
        @DisplayName("should normalize plate number in update request")
        void shouldNormalizePlateNumberInUpdateRequest() {
            when(vehicleRepository.findByIdAndTenantId(VEHICLE_ID, TENANT_ID))
                    .thenReturn(Optional.of(vehicle));
            when(vehicleRepository.existsByPlateNumberAndTenantId("XYZ789", TENANT_ID))
                    .thenReturn(false);
            when(vehicleRepository.save(vehicle)).thenReturn(vehicle);
            when(vehicleMapper.toResponse(vehicle)).thenReturn(vehicleResponse);

            vehicleService.updateVehicle(VEHICLE_ID, updateRequest);

            assertThat(updateRequest.getPlateNumber()).isEqualTo("XYZ789");
            verify(vehicleRepository).existsByPlateNumberAndTenantId("XYZ789", TENANT_ID);
        }

        @Test
        @DisplayName("should skip plate uniqueness check when plate number is not being changed")
        void shouldSkipPlateUniquenessCheckWhenPlateUnchanged() {
            UpdateVehicleRequest sameplate = UpdateVehicleRequest.builder()
                    .plateNumber("ABC123")
                    .make("Honda")
                    .build();

            when(vehicleRepository.findByIdAndTenantId(VEHICLE_ID, TENANT_ID))
                    .thenReturn(Optional.of(vehicle));
            when(vehicleRepository.save(vehicle)).thenReturn(vehicle);
            when(vehicleMapper.toResponse(vehicle)).thenReturn(vehicleResponse);

            vehicleService.updateVehicle(VEHICLE_ID, sameplate);

            // normalized plate "ABC123" equals vehicle.getPlateNumber() "ABC123", so no duplicate check
            verify(vehicleRepository, never()).existsByPlateNumberAndTenantId(any(), any());
            verify(vehicleMapper).updateEntity(sameplate, vehicle);
            verify(vehicleRepository).save(vehicle);
        }

        @Test
        @DisplayName("should skip plate normalization when plate number is null in request")
        void shouldSkipPlateNormalizationWhenPlateIsNull() {
            UpdateVehicleRequest noPlateRequest = UpdateVehicleRequest.builder()
                    .make("Honda")
                    .model("Civic")
                    .build();

            when(vehicleRepository.findByIdAndTenantId(VEHICLE_ID, TENANT_ID))
                    .thenReturn(Optional.of(vehicle));
            when(vehicleRepository.save(vehicle)).thenReturn(vehicle);
            when(vehicleMapper.toResponse(vehicle)).thenReturn(vehicleResponse);

            vehicleService.updateVehicle(VEHICLE_ID, noPlateRequest);

            verify(vehicleRepository, never()).existsByPlateNumberAndTenantId(any(), any());
            verify(vehicleMapper).updateEntity(noPlateRequest, vehicle);
            verify(vehicleRepository).save(vehicle);
        }

        @Test
        @DisplayName("should throw DuplicateResourceException when updated plate already exists for another vehicle")
        void shouldThrowDuplicateResourceExceptionWhenUpdatedPlateExists() {
            when(vehicleRepository.findByIdAndTenantId(VEHICLE_ID, TENANT_ID))
                    .thenReturn(Optional.of(vehicle));
            when(vehicleRepository.existsByPlateNumberAndTenantId("XYZ789", TENANT_ID))
                    .thenReturn(true);

            assertThatThrownBy(() -> vehicleService.updateVehicle(VEHICLE_ID, updateRequest))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("Vehicle")
                    .hasMessageContaining("plateNumber")
                    .hasMessageContaining("XYZ789");

            verify(vehicleRepository).findByIdAndTenantId(VEHICLE_ID, TENANT_ID);
            verify(vehicleRepository).existsByPlateNumberAndTenantId("XYZ789", TENANT_ID);
            verify(vehicleMapper, never()).updateEntity(any(), any());
            verify(vehicleRepository, never()).save(any(Vehicle.class));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when vehicle not found for update")
        void shouldThrowResourceNotFoundExceptionWhenNotFoundForUpdate() {
            UUID nonExistentId = UUID.fromString("00000000-0000-0000-0000-000000000099");
            when(vehicleRepository.findByIdAndTenantId(nonExistentId, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> vehicleService.updateVehicle(nonExistentId, updateRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Vehicle")
                    .hasMessageContaining("id");

            verify(vehicleRepository).findByIdAndTenantId(nonExistentId, TENANT_ID);
            verify(vehicleMapper, never()).updateEntity(any(), any());
            verify(vehicleRepository, never()).save(any(Vehicle.class));
        }
    }

    // ========================================================================
    // deleteVehicle
    // ========================================================================

    @Nested
    @DisplayName("deleteVehicle")
    class DeleteVehicle {

        @Test
        @DisplayName("should soft-delete vehicle by setting status REMOVED, deleted=true, active=false")
        void shouldSoftDeleteVehicle() {
            when(vehicleRepository.findByIdAndTenantId(VEHICLE_ID, TENANT_ID))
                    .thenReturn(Optional.of(vehicle));

            vehicleService.deleteVehicle(VEHICLE_ID);

            assertThat(vehicle.isDeleted()).isTrue();
            assertThat(vehicle.isActive()).isFalse();
            assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.REMOVED);

            verify(vehicleRepository).findByIdAndTenantId(VEHICLE_ID, TENANT_ID);
            verify(vehicleRepository).save(vehicle);
        }

        @Test
        @DisplayName("should persist soft-deleted vehicle via repository save")
        void shouldPersistSoftDeletedVehicle() {
            when(vehicleRepository.findByIdAndTenantId(VEHICLE_ID, TENANT_ID))
                    .thenReturn(Optional.of(vehicle));

            vehicleService.deleteVehicle(VEHICLE_ID);

            verify(vehicleRepository).save(vehicle);
            assertThat(vehicle.isDeleted()).isTrue();
            assertThat(vehicle.isActive()).isFalse();
            assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.REMOVED);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when vehicle not found for delete")
        void shouldThrowResourceNotFoundExceptionWhenNotFoundForDelete() {
            UUID nonExistentId = UUID.fromString("00000000-0000-0000-0000-000000000099");
            when(vehicleRepository.findByIdAndTenantId(nonExistentId, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> vehicleService.deleteVehicle(nonExistentId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Vehicle")
                    .hasMessageContaining("id");

            verify(vehicleRepository).findByIdAndTenantId(nonExistentId, TENANT_ID);
            verify(vehicleRepository, never()).save(any(Vehicle.class));
        }
    }
}
