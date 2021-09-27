package com.rarible.protocol.nft.core.converters.dto

import com.rarible.protocol.dto.NftItemDto
import com.rarible.protocol.nft.core.model.ExtendedItem
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
object ExtendedItemDtoConverter : Converter<ExtendedItem, NftItemDto> {
    override fun convert(source: ExtendedItem): NftItemDto {
        val (item, meta) = source
        return NftItemDto(
            id = item.id.decimalStringValue,
            contract = item.token,
            tokenId = item.tokenId.value,
            creators = item.creators.map { PartDtoConverter.convert(it) },
            supply = item.supply.value,
            lazySupply = item.lazySupply.value,
            owners = item.owners,
            royalties = item.royalties.map { PartDtoConverter.convert(it) },
            date = item.date,
            pending = item.pending.map { ItemTransferDtoConverter.convert(it) },
            deleted = item.deleted,
            meta = NftItemMetaDtoConverter.convert(meta)
        )
    }
}
