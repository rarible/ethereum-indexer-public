package com.rarible.protocol.nft.core.converters.dto

import com.rarible.protocol.dto.ItemHistoryDto
import com.rarible.protocol.nft.core.model.ItemHistory
import com.rarible.protocol.nft.core.model.ItemRoyalty
import com.rarible.protocol.nft.core.model.ItemTransfer
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
object ItemHistoryDtoConverter : Converter<ItemHistory, ItemHistoryDto> {
    override fun convert(source: ItemHistory): ItemHistoryDto {
        return when (source) {
            is ItemRoyalty -> {
                ItemRoyaltyDtoConverter.convert(source)
            }
            is ItemTransfer -> {
                ItemTransferDtoConverter.convert(source)
            }
            else -> throw IllegalArgumentException("Unsupported ItemHistory type ${source.javaClass}")
        }
    }
}

