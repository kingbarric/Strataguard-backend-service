package com.estatekit.api.controller;

import com.estatekit.core.dto.common.ApiResponse;
import com.estatekit.core.dto.exitpass.ExitPassResponse;
import com.estatekit.service.gate.ExitPassService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/exit-pass")
@RequiredArgsConstructor
@Tag(name = "Exit Pass", description = "Generate dynamic exit pass QR tokens")
public class ExitPassController {

    private final ExitPassService exitPassService;

    @GetMapping("/vehicle/{vehicleId}")
    @Operation(summary = "Generate a dynamic exit pass token for a vehicle")
    public ResponseEntity<ApiResponse<ExitPassResponse>> generateExitPass(@PathVariable UUID vehicleId) {
        ExitPassResponse response = exitPassService.generateExitPass(vehicleId);
        return ResponseEntity.ok(ApiResponse.success(response, "Exit pass generated successfully"));
    }
}
