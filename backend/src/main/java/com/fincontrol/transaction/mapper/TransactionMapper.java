package com.fincontrol.transaction.mapper;

import com.fincontrol.transaction.dto.TransactionDtos;
import com.fincontrol.transaction.entity.TransactionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransactionMapper {
    @Mapping(source = "account.id", target = "accountId")
    @Mapping(source = "account.name", target = "accountName")
    @Mapping(source = "card.id", target = "cardId")
    @Mapping(source = "card.name", target = "cardName")
    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(source = "category.name", target = "categoryName")
    TransactionDtos.Response toResponse(TransactionEntity entity);
}
