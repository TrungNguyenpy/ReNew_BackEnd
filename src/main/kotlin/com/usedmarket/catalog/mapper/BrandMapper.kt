package com.usedmarket.catalog.mapper

import com.usedmarket.catalog.dto.BrandResponse
import com.usedmarket.catalog.entity.Brand
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(componentModel = "spring")
interface BrandMapper {

    // Kotlin compiles `isActive: Boolean` to a getter named isActive() (not getIsActive()),
    // which MapStruct's default JavaBean matching interprets as property "active", not
    // "isActive" — silently leaving this field unmapped (defaults to false). Same fix as
    // CategoryMapper: map it explicitly via the real getter.
    @Mapping(target = "isActive", expression = "java(brand.isActive())")
    fun toResponse(brand: Brand): BrandResponse
}
