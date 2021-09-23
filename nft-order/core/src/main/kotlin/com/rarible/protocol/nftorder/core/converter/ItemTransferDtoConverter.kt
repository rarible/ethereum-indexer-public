package com.rarible.protocol.nftorder.core.converter

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.ItemTransferDto
import com.rarible.protocol.nftorder.core.model.ItemTransfer

object ItemTransferDtoConverter {

    fun convert(list: List<ItemTransferDto>): List<ItemTransfer> {
        return list.map {
            ItemTransfer(
                it.owner,
                it.contract,
                EthUInt256(it.tokenId),
                it.date,
                it.from,
                EthUInt256(it.value)
            )
        }
    }
}