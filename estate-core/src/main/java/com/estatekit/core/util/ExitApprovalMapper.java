package com.estatekit.core.util;

import com.estatekit.core.dto.approval.ExitApprovalResponse;
import com.estatekit.core.entity.ExitApprovalRequest;
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
