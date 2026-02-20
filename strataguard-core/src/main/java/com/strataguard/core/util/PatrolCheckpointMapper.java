package com.strataguard.core.util;

import com.strataguard.core.dto.security.CheckpointResponse;
import com.strataguard.core.dto.security.CreateCheckpointRequest;
import com.strataguard.core.dto.security.UpdateCheckpointRequest;
import com.strataguard.core.entity.PatrolCheckpoint;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface PatrolCheckpointMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "active", expression = "java(true)")
    PatrolCheckpoint toEntity(CreateCheckpointRequest request);

    CheckpointResponse toResponse(PatrolCheckpoint checkpoint);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "active", ignore = true)
    void updateEntity(UpdateCheckpointRequest request, @MappingTarget PatrolCheckpoint checkpoint);
}
