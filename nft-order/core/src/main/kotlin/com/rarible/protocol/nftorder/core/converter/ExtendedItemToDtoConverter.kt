package com.rarible.protocol.nftorder.core.converter

import com.rarible.protocol.dto.NftOrderItemDto
import com.rarible.protocol.dto.PartDto
import com.rarible.protocol.nftorder.core.model.ExtendedItem
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
object ExtendedItemToDtoConverter : Converter<ExtendedItem, NftOrderItemDto> {
    override fun convert(extendedItem: ExtendedItem): NftOrderItemDto {
        val (item, meta) = extendedItem
        return item.run {
            NftOrderItemDto(
                id.decimalStringValue,
                token,
                tokenId.value,
                unlockable,
                creators.map { PartDto(it.account, it.value) },
                supply.value,
                lazySupply.value,
                owners,
                royalties.map { PartDto(it.account, it.value) },
                date,
                pending.map { ItemTransferToDtoConverter.convert(it) },
                meta,
                bestSellOrder,
                bestBidOrder,
                totalStock,
                sellers
            )
        }
    }
}