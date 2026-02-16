package com.estatekit.core.util;

import com.estatekit.core.dto.visitor.CreateVisitorRequest;
import com.estatekit.core.dto.visitor.UpdateVisitorRequest;
import com.estatekit.core.dto.visitor.VisitorResponse;
import com.estatekit.core.entity.Visitor;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface VisitorMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "invitedBy", ignore = true)
    @Mapping(target = "status", expression = "java(com.estatekit.core.enums.VisitorStatus.PENDING)")
    Visitor toEntity(CreateVisitorRequest request);

    @Mapping(target = "invitedByName", ignore = true)
    VisitorResponse toResponse(Visitor visitor);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "invitedBy", ignore = true)
    @Mapping(target = "visitorType", ignore = true)
    @Mapping(target = "status", ignore = true)
    void updateEntity(UpdateVisitorRequest request, @MappingTarget Visitor visitor);
}
