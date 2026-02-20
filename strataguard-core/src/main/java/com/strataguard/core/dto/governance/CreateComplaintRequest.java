package com.strataguard.core.dto.governance;

import com.strataguard.core.enums.ComplaintCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class CreateComplaintRequest {
    @NotNull
    private UUID estateId;

    @NotBlank
    private String title;

    @NotBlank
    private String description;

    @NotNull
    private ComplaintCategory category;

    private boolean anonymous;
    private String attachmentUrl;
}
