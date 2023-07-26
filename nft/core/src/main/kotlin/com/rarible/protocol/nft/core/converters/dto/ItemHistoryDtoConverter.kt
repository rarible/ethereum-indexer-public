package com.rarible.protocol.nft.core.converters.dto

import com.rarible.protocol.dto.ItemHistoryDto
import com.rarible.protocol.nft.core.model.ItemHistory
import com.rarible.protocol.nft.core.model.ItemRoyalty
import com.rarible.protocol.nft.core.model.ItemTransfer

object ItemHistoryDtoConverter {

    fun convert(source: ItemHistory): ItemHistoryDto {
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
