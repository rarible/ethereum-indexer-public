package com.rarible.protocol.nftorder.core.converter

import com.rarible.protocol.dto.ItemTransferDto
import com.rarible.protocol.nftorder.core.model.ItemTransfer

object ItemTransferToDtoConverter {

    fun convert(source: ItemTransfer): ItemTransferDto {
        return ItemTransferDto(
            owner = source.owner,
            contract = source.token,
            tokenId = source.tokenId.value,
            date = source.date,
            value = source.value.value,
            from = source.from
        )
    }
}