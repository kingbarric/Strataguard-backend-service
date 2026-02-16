package com.strataguard.core.util;

import com.strataguard.core.dto.approval.ExitApprovalResponse;
import com.strataguard.core.entity.ExitApprovalRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ExitApprovalMapper {

    @Mapping(target = "plateNumber", ignore = true)
    @Mapping(target = "vehicleMake", ignore = true)
    @Mapping(target = "vehicleModel", ignore = true)
    @Mapping(target = "vehicleColor", ignore = true)
    ExitApprovalResponse toResponse(ExitApprovalRequest request);
}
