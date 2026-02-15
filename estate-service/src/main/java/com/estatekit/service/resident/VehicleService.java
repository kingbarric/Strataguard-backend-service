package com.estatekit.service.resident;

import com.estatekit.core.config.TenantContext;
import com.estatekit.core.dto.common.PagedResponse;
import com.estatekit.core.dto.vehicle.*;
import com.estatekit.core.entity.Vehicle;
import com.estatekit.core.enums.VehicleStatus;
import com.estatekit.core.exception.DuplicateResourceException;
import com.estatekit.core.exception.ResourceNotFoundException;
import com.estatekit.core.util.CsvParserUtils;
import com.estatekit.core.util.PlateNumberUtils;
import com.estatekit.core.util.VehicleMapper;
import com.estatekit.infrastructure.repository.ResidentRepository;
import com.estatekit.infrastructure.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final ResidentRepository residentRepository;
    private final VehicleMapper vehicleMapper;

    public VehicleResponse createVehicle(CreateVehicleRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        // Verify resident exists
        residentRepository.findByIdAndTenantId(request.getResidentId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Resident", "id", request.getResidentId()));

        // Normalize and check plate number
        String normalizedPlate = PlateNumberUtils.normalize(request.getPlateNumber());
        if (vehicleRepository.existsByPlateNumberAndTenantId(normalizedPlate, tenantId)) {
            throw new DuplicateResourceException("Vehicle", "plateNumber", normalizedPlate);
        }

        Vehicle vehicle = vehicleMapper.toEntity(request);
        vehicle.setTenantId(tenantId);
        vehicle.setPlateNumber(normalizedPlate);
        vehicle.setQrStickerCode("VEH-" + UUID.randomUUID());

        Vehicle saved = vehicleRepository.save(vehicle);
        log.info("Created vehicle: {} with plate: {} for tenant: {}", saved.getId(), normalizedPlate, tenantId);
        return vehicleMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public VehicleResponse getVehicle(UUID vehicleId) {
        UUID tenantId = TenantContext.requireTenantId();
        Vehicle vehicle = vehicleRepository.findByIdAndTenantId(vehicleId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", "id", vehicleId));
        return vehicleMapper.toResponse(vehicle);
    }

    @Transactional(readOnly = true)
    public PagedResponse<VehicleResponse> getAllVehicles(Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<Vehicle> page = vehicleRepository.findAllByTenantId(tenantId, pageable);
        return toPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<VehicleResponse> getVehiclesByResident(UUID residentId, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        residentRepository.findByIdAndTenantId(residentId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Resident", "id", residentId));

        Page<Vehicle> page = vehicleRepository.findByResidentIdAndTenantId(residentId, tenantId, pageable);
        return toPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<VehicleResponse> searchVehicles(String search, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<Vehicle> page = vehicleRepository.searchByTenantId(tenantId, search, pageable);
        return toPagedResponse(page);
    }

    public VehicleResponse updateVehicle(UUID vehicleId, UpdateVehicleRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        Vehicle vehicle = vehicleRepository.findByIdAndTenantId(vehicleId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", "id", vehicleId));

        // If plate number is being updated, normalize and check uniqueness
        if (request.getPlateNumber() != null) {
            String normalizedPlate = PlateNumberUtils.normalize(request.getPlateNumber());
            if (!normalizedPlate.equals(vehicle.getPlateNumber())
                    && vehicleRepository.existsByPlateNumberAndTenantId(normalizedPlate, tenantId)) {
                throw new DuplicateResourceException("Vehicle", "plateNumber", normalizedPlate);
            }
            request.setPlateNumber(normalizedPlate);
        }

        vehicleMapper.updateEntity(request, vehicle);
        Vehicle updated = vehicleRepository.save(vehicle);
        log.info("Updated vehicle: {} for tenant: {}", vehicleId, tenantId);
        return vehicleMapper.toResponse(updated);
    }

    public void deleteVehicle(UUID vehicleId) {
        UUID tenantId = TenantContext.requireTenantId();
        Vehicle vehicle = vehicleRepository.findByIdAndTenantId(vehicleId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", "id", vehicleId));

        vehicle.setDeleted(true);
        vehicle.setActive(false);
        vehicle.setStatus(VehicleStatus.REMOVED);
        vehicleRepository.save(vehicle);
        log.info("Soft-deleted vehicle: {} for tenant: {}", vehicleId, tenantId);
    }

    public BulkImportResponse bulkImport(InputStream csvInputStream) {
        UUID tenantId = TenantContext.requireTenantId();

        List<VehicleCsvRow> rows;
        try {
            rows = CsvParserUtils.parseVehicleCsv(csvInputStream);
        } catch (IOException e) {
            log.error("Failed to parse CSV: {}", e.getMessage());
            return BulkImportResponse.builder()
                    .totalRows(0)
                    .successCount(0)
                    .failureCount(0)
                    .errors(List.of("Failed to parse CSV file: " + e.getMessage()))
                    .build();
        }

        int successCount = 0;
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < rows.size(); i++) {
            VehicleCsvRow row = rows.get(i);
            try {
                String normalizedPlate = PlateNumberUtils.normalize(row.getPlateNumber());

                if (vehicleRepository.existsByPlateNumberAndTenantId(normalizedPlate, tenantId)) {
                    errors.add("Row " + (i + 2) + ": Duplicate plate number '" + normalizedPlate + "'");
                    continue;
                }

                if (residentRepository.findByIdAndTenantId(row.getResidentId(), tenantId).isEmpty()) {
                    errors.add("Row " + (i + 2) + ": Resident not found with id '" + row.getResidentId() + "'");
                    continue;
                }

                Vehicle vehicle = new Vehicle();
                vehicle.setTenantId(tenantId);
                vehicle.setResidentId(row.getResidentId());
                vehicle.setPlateNumber(normalizedPlate);
                vehicle.setMake(row.getMake());
                vehicle.setModel(row.getModel());
                vehicle.setVehicleType(row.getVehicleType());
                vehicle.setColor(row.getColor());
                vehicle.setStickerNumber(row.getStickerNumber());
                vehicle.setQrStickerCode("VEH-" + UUID.randomUUID());

                vehicleRepository.save(vehicle);
                successCount++;
            } catch (Exception e) {
                errors.add("Row " + (i + 2) + ": " + e.getMessage());
            }
        }

        log.info("Bulk import completed: {} success, {} failures out of {} rows for tenant: {}",
                successCount, errors.size(), rows.size(), tenantId);

        return BulkImportResponse.builder()
                .totalRows(rows.size())
                .successCount(successCount)
                .failureCount(errors.size())
                .errors(errors)
                .build();
    }

    private PagedResponse<VehicleResponse> toPagedResponse(Page<Vehicle> page) {
        return PagedResponse.<VehicleResponse>builder()
                .content(page.getContent().stream().map(vehicleMapper::toResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .first(page.isFirst())
                .build();
    }
}
