package com.strataguard.core.util;

import com.strataguard.core.dto.security.CreateCameraRequest;
import com.strataguard.core.dto.security.UpdateCameraRequest;
import com.strataguard.core.dto.security.CameraResponse;
import com.strataguard.core.entity.CctvCamera;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface CctvCameraMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "lastOnlineAt", ignore = true)
    @Mapping(target = "active", expression = "java(true)")
    CctvCamera toEntity(CreateCameraRequest request);

    CameraResponse toResponse(CctvCamera camera);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "lastOnlineAt", ignore = true)
    @Mapping(target = "active", ignore = true)
    void updateEntity(UpdateCameraRequest request, @MappingTarget CctvCamera camera);
}
