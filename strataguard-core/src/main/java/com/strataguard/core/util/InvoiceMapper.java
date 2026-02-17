package com.strataguard.core.util;

import com.strataguard.core.dto.billing.InvoiceResponse;
import com.strataguard.core.entity.LevyInvoice;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InvoiceMapper {

    @Mapping(target = "levyTypeName", ignore = true)
    @Mapping(target = "unitNumber", ignore = true)
    @Mapping(target = "residentName", ignore = true)
    InvoiceResponse toResponse(LevyInvoice invoice);
}
