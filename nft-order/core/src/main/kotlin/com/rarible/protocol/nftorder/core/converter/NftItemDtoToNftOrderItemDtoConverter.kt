package com.rarible.protocol.nftorder.core.converter

import com.rarible.core.common.nowMillis
import com.rarible.protocol.dto.NftItemDto
import com.rarible.protocol.dto.NftOrderItemDto
import java.math.BigInteger

object NftItemDtoToNftOrderItemDtoConverter {

    fun convert(nftItem: NftItemDto): NftOrderItemDto {
        return NftOrderItemDto(
            id = nftItem.id,
            contract = nftItem.contract,
            tokenId = nftItem.tokenId,
            creators = nftItem.creators,
            supply = nftItem.supply,
            lazySupply = nftItem.lazySupply,
            royalties = nftItem.royalties,
            owners = nftItem.owners,
            date = nftItem.date ?: nowMillis(),
            pending = (nftItem.pending ?: emptyList()),
            meta = nftItem.meta,
            // Default enrichment data
            sellers = 0,
            totalStock = BigInteger.ZERO,
            bestSellOrder = null,
            bestBidOrder = null,
            unlockable = false
        )
    }
}