package com.rarible.protocol.nft.core.converters.dto

import com.rarible.protocol.dto.NftItemDto
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemMeta
import org.springframework.stereotype.Component

@Component
object ItemDtoConverter {
    fun convert(item: Item, meta: ItemMeta?): NftItemDto {
        return NftItemDto(
            id = item.id.decimalStringValue,
            contract = item.token,
            tokenId = item.tokenId.value,
            creators = item.creators.map { PartDtoConverter.convert(it) },
            supply = item.supply.value,
            lazySupply = item.lazySupply.value,
            owners = item.owners,
            royalties =  item.royalties.map { PartDtoConverter.convert(it) },
            date = item.date,
            pending = item.pending.map { ItemTransferDtoConverter.convert(it) },
            deleted = item.deleted,
            meta = meta?.let { NftItemMetaDtoConverter.convert(it) }
        )
    }
}

