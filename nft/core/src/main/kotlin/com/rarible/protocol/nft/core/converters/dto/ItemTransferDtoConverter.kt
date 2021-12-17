package com.rarible.protocol.nft.core.converters.dto

import com.rarible.protocol.dto.ItemTransferDto
import com.rarible.protocol.nft.core.model.ItemEvent
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemTransfer
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
object ItemTransferDtoConverter : Converter<ItemTransfer, ItemTransferDto> {

    override fun convert(source: ItemTransfer): ItemTransferDto {
        return ItemTransferDto(
            owner = source.owner,
            contract = source.token,
            tokenId = source.tokenId.value,
            date = source.date,
            value = source.value.value,
            from = source.from
        )
    }

    fun convert(source: ItemEvent): ItemTransferDto? {
        val itemId = ItemId.parseId(source.entityId)
        return when (source) {
            is ItemEvent.ItemBurnEvent -> ItemTransferDto(
                from = source.owner,
                owner = Address.ZERO(),
                contract = itemId.token,
                tokenId = itemId.tokenId.value,
                date = source.log.createdAt,
                value = source.supply.value,
            )
            is ItemEvent.ItemMintEvent -> ItemTransferDto(
                owner = source.owner,
                from = Address.ZERO(),
                contract = itemId.token,
                tokenId = itemId.tokenId.value,
                date = source.log.createdAt,
                value = source.supply.value,
            )
            is ItemEvent.ItemTransferEvent -> ItemTransferDto(
                owner = source.to,
                from = source.from,
                contract = itemId.token,
                tokenId = itemId.tokenId.value,
                date = source.log.createdAt,
                value = source.value.value,
            )
            is ItemEvent.LazyItemBurnEvent,
            is ItemEvent.LazyItemMintEvent,
            is ItemEvent.ItemCreatorsEvent -> null
        }
    }
}
