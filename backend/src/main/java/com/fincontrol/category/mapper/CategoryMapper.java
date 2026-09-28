package com.fincontrol.category.mapper;

import com.fincontrol.category.dto.CategoryDtos;
import com.fincontrol.category.entity.CategoryEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    CategoryDtos.Response toResponse(CategoryEntity entity);
}
