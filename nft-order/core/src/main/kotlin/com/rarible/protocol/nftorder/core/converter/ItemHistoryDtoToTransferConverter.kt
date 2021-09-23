package com.rarible.protocol.nftorder.core.converter

import com.rarible.protocol.dto.ItemHistoryDto
import com.rarible.protocol.dto.ItemTransferDto
import com.rarible.protocol.nftorder.core.model.ItemTransfer

object ItemHistoryDtoToTransferConverter {

    fun convert(list: List<ItemHistoryDto>): List<ItemTransfer> {
        val transfers = list.map { it as ItemTransferDto }
        return ItemTransferDtoConverter.convert(transfers)
    }

}