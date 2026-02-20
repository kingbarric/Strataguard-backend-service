package com.strataguard.core.util;

import com.strataguard.core.dto.governance.ArtisanResponse;
import com.strataguard.core.dto.governance.CreateArtisanRequest;
import com.strataguard.core.dto.governance.UpdateArtisanRequest;
import com.strataguard.core.entity.Artisan;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ArtisanMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "status", expression = "java(com.strataguard.core.enums.ArtisanStatus.ACTIVE)")
    @Mapping(target = "verified", constant = "false")
    @Mapping(target = "totalJobs", constant = "0")
    @Mapping(target = "totalRating", constant = "0.0")
    @Mapping(target = "ratingCount", constant = "0")
    @Mapping(target = "averageRating", constant = "0.0")
    Artisan toEntity(CreateArtisanRequest request);

    ArtisanResponse toResponse(Artisan artisan);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "estateId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "verified", ignore = true)
    @Mapping(target = "totalJobs", ignore = true)
    @Mapping(target = "totalRating", ignore = true)
    @Mapping(target = "ratingCount", ignore = true)
    @Mapping(target = "averageRating", ignore = true)
    void updateEntity(UpdateArtisanRequest request, @MappingTarget Artisan artisan);
}
