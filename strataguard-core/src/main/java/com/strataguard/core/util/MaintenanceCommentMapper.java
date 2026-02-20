package com.strataguard.core.util;

import com.strataguard.core.dto.maintenance.CommentResponse;
import com.strataguard.core.entity.MaintenanceComment;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MaintenanceCommentMapper {

    CommentResponse toResponse(MaintenanceComment comment);
}
