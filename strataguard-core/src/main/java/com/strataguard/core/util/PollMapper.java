package com.strataguard.core.util;

import com.strataguard.core.dto.governance.PollOptionResponse;
import com.strataguard.core.dto.governance.PollResponse;
import com.strataguard.core.entity.Poll;
import com.strataguard.core.entity.PollOption;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PollMapper {

    @Mapping(target = "options", ignore = true)
    PollResponse toResponse(Poll poll);

    @Mapping(target = "percentage", constant = "0.0")
    PollOptionResponse toOptionResponse(PollOption option);
}
