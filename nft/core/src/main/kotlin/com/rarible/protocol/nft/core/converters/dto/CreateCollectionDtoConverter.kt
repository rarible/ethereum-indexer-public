package com.rarible.protocol.nft.core.converters.dto

import com.rarible.protocol.dto.NftCreateCollectionDto
import com.rarible.protocol.nft.core.model.CreateCollection
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
object CreateCollectionDtoConverter : Converter<CreateCollection, NftCreateCollectionDto> {
    override fun convert(source: CreateCollection): NftCreateCollectionDto {
        return NftCreateCollectionDto(
            id = source.id,
            owner = source.owner,
            name = source.name,
            symbol = source.symbol
        )
    }
}
