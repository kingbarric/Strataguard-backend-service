package com.strataguard.core.util;

import com.strataguard.core.dto.governance.AnnouncementResponse;
import com.strataguard.core.dto.governance.CreateAnnouncementRequest;
import com.strataguard.core.dto.governance.UpdateAnnouncementRequest;
import com.strataguard.core.entity.Announcement;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface AnnouncementMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "postedBy", ignore = true)
    @Mapping(target = "postedByName", ignore = true)
    @Mapping(target = "publishedAt", ignore = true)
    @Mapping(target = "published", ignore = true)
    @Mapping(target = "priority", expression = "java(request.getPriority() != null ? request.getPriority() : com.strataguard.core.enums.AnnouncementPriority.NORMAL)")
    Announcement toEntity(CreateAnnouncementRequest request);

    AnnouncementResponse toResponse(Announcement announcement);

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
    @Mapping(target = "postedBy", ignore = true)
    @Mapping(target = "postedByName", ignore = true)
    @Mapping(target = "publishedAt", ignore = true)
    @Mapping(target = "published", ignore = true)
    void updateEntity(UpdateAnnouncementRequest request, @MappingTarget Announcement announcement);
}
