package com.rarible.protocol.nftorder.core.converter

import com.rarible.protocol.dto.*
import com.rarible.protocol.nftorder.core.model.ItemTransfer
import com.rarible.protocol.nftorder.core.model.Ownership
import io.daonomic.rpc.domain.Word

object OwnershipToDtoConverter {

    fun convert(source: Ownership, orders: Map<Word, OrderDto>): NftOrderOwnershipDto {
        return NftOrderOwnershipDto(
            id = source.id.decimalStringValue,
            contract = source.contract,
            tokenId = source.tokenId.value,
            owner = source.owner,
            creators = source.creators.map { PartDto(it.account, it.value) },
            value = source.value.value,
            lazyValue = source.lazyValue.value,
            date = source.date,
            pending = source.pending.map { convert(it) },
            bestSellOrder = source.bestSellOrder?.let { orders[it.hash] }
        )
    }

    private fun convert(source: ItemTransfer): ItemHistoryDto =
        ItemTransferDto(
            owner = source.owner,
            contract = source.token,
            tokenId = source.tokenId.value,
            date = source.date,
            value = source.value.value,
            from = source.from
        )
}