package com.estatekit.core.dto.gate;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GateExitRequest {

    @NotBlank(message = "QR sticker code is required")
    private String qrStickerCode;

    @NotBlank(message = "Exit pass token is required")
    private String exitPassToken;

    private String note;
}
