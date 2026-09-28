package com.fincontrol.account.mapper;

import com.fincontrol.account.dto.AccountDtos;
import com.fincontrol.account.entity.AccountEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AccountMapper {
    AccountDtos.Details toDetails(AccountEntity entity);
}
