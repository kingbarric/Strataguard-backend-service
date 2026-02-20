package com.strataguard.core.util;

import com.strataguard.core.dto.governance.ArtisanRatingResponse;
import com.strataguard.core.entity.ArtisanRating;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ArtisanRatingMapper {

    ArtisanRatingResponse toResponse(ArtisanRating rating);
}
