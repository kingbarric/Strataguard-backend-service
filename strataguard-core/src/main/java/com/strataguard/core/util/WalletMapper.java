package com.strataguard.core.util;

import com.strataguard.core.dto.payment.WalletResponse;
import com.strataguard.core.dto.payment.WalletTransactionResponse;
import com.strataguard.core.entity.ResidentWallet;
import com.strataguard.core.entity.WalletTransaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WalletMapper {

    @Mapping(target = "residentName", ignore = true)
    WalletResponse toResponse(ResidentWallet wallet);

    WalletTransactionResponse toTransactionResponse(WalletTransaction transaction);
}
