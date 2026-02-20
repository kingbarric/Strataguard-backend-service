package com.strataguard.core.dto.governance;

import com.strataguard.core.enums.ArtisanCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class CreateArtisanRequest {
    @NotNull
    private UUID estateId;

    @NotBlank
    private String name;

    @NotBlank
    private String phone;

    private String email;

    @NotNull
    private ArtisanCategory category;

    private String specialization;
    private String description;
    private String photoUrl;
    private String address;
    private String notes;
}
