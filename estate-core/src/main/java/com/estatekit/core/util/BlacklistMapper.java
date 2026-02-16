package com.estatekit.core.util;

import com.estatekit.core.dto.blacklist.CreateBlacklistRequest;
import com.estatekit.core.dto.blacklist.UpdateBlacklistRequest;
import com.estatekit.core.dto.blacklist.BlacklistResponse;
import com.estatekit.core.entity.Blacklist;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface BlacklistMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "addedBy", ignore = true)
    @Mapping(target = "active", expression = "java(true)")
    Blacklist toEntity(CreateBlacklistRequest request);

    BlacklistResponse toResponse(Blacklist blacklist);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "addedBy", ignore = true)
    void updateEntity(UpdateBlacklistRequest request, @MappingTarget Blacklist blacklist);
}
