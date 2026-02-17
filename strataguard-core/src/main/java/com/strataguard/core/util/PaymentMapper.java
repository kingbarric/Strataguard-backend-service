package com.strataguard.core.util;

import com.strataguard.core.dto.payment.PaymentResponse;
import com.strataguard.core.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    @Mapping(target = "invoiceNumber", ignore = true)
    PaymentResponse toResponse(Payment payment);
}
