package com.usedmarket.catalog.mapper

import com.usedmarket.catalog.dto.CategoryResponse
import com.usedmarket.catalog.entity.Category
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(componentModel = "spring")
interface CategoryMapper {

    @Mapping(target = "parentId", expression = "java(category.getParent() != null ? category.getParent().getId() : null)")
    @Mapping(target = "isActive", expression = "java(category.isActive())")
    @Mapping(target = "children", expression = "java(java.util.Collections.emptyList())")
    fun toResponse(category: Category): CategoryResponse
}
