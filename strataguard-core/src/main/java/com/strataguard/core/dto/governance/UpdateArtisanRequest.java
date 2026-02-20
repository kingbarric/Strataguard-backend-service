package com.strataguard.core.dto.governance;

import com.strataguard.core.enums.ArtisanCategory;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdateArtisanRequest {
    private String name;
    private String phone;
    private String email;
    private ArtisanCategory category;
    private String specialization;
    private String description;
    private String photoUrl;
    private String address;
    private String notes;
}
