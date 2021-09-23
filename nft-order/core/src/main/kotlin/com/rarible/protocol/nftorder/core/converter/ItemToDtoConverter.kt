package com.rarible.protocol.nftorder.core.converter

import com.rarible.protocol.dto.NftItemMetaDto
import com.rarible.protocol.dto.NftOrderItemDto
import com.rarible.protocol.dto.OrderDto
import com.rarible.protocol.dto.PartDto
import com.rarible.protocol.nftorder.core.model.Item
import io.daonomic.rpc.domain.Word

object ItemToDtoConverter {

    fun convert(item: Item, meta: NftItemMetaDto, orders: Map<Word, OrderDto>): NftOrderItemDto {
        return NftOrderItemDto(
            id = item.id.decimalStringValue,
            contract = item.token,
            tokenId = item.tokenId.value,
            unlockable = item.unlockable,
            creators = item.creators.map { PartDto(it.account, it.value) },
            supply = item.supply.value,
            lazySupply = item.lazySupply.value,
            owners = item.owners,
            royalties = item.royalties.map { PartDto(it.account, it.value) },
            date = item.date,
            pending = item.pending.map { ItemTransferToDtoConverter.convert(it) },
            meta = meta,
            bestSellOrder = item.bestSellOrder?.let { orders[it.hash] },
            bestBidOrder = item.bestBidOrder?.let { orders[it.hash] },
            totalStock = item.totalStock,
            sellers = item.sellers
        )

    }
}
