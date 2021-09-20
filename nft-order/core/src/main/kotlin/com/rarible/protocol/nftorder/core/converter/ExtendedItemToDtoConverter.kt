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
                id = id.decimalStringValue,
                contract = token,
                tokenId = tokenId.value,
                unlockable = unlockable,
                creators = creators.map { PartDto(it.account, it.value) },
                supply = supply.value,
                lazySupply = lazySupply.value,
                owners = owners,
                royalties = royalties.map { PartDto(it.account, it.value) },
                date = date,
                pending = pending.map { ItemTransferToDtoConverter.convert(it) },
                meta = meta,
                bestSellOrder = bestSellOrder,
                bestBidOrder = bestBidOrder,
                totalStock = totalStock,
                sellers = sellers
            )
        }
    }
}
