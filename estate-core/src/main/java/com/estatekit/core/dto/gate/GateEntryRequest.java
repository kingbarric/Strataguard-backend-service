package com.estatekit.core.dto.gate;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GateEntryRequest {

    @NotBlank(message = "QR sticker code is required")
    private String qrStickerCode;

    private String note;
}
