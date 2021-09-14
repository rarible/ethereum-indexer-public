package com.rarible.protocol.nftorder.core.converter

import com.rarible.protocol.dto.NftOrderDeletedItemDto
import com.rarible.protocol.dto.NftOrderItemDeleteEventDto
import com.rarible.protocol.dto.NftOrderItemEventDto
import com.rarible.protocol.dto.NftOrderItemUpdateEventDto
import com.rarible.protocol.nftorder.core.event.ItemEvent
import com.rarible.protocol.nftorder.core.event.ItemEventDelete
import com.rarible.protocol.nftorder.core.event.ItemEventUpdate
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
object ItemEventToDtoConverter : Converter<ItemEvent, NftOrderItemEventDto> {

    override fun convert(source: ItemEvent): NftOrderItemEventDto {
        return when (source) {
            is ItemEventUpdate -> NftOrderItemUpdateEventDto(
                source.id,
                source.item.item.id.decimalStringValue,
                ExtendedItemToDtoConverter.convert(source.item)
            )
            is ItemEventDelete -> {
                NftOrderItemDeleteEventDto(
                    source.id,
                    source.itemId.decimalStringValue,
                    NftOrderDeletedItemDto(
                        id = source.itemId.stringValue,
                        token = source.itemId.token,
                        tokenId = source.itemId.tokenId.value
                    )
                )
            }
        }
    }
}
