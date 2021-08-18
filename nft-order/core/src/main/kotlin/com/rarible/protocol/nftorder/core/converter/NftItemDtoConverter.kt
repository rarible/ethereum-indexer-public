package com.rarible.protocol.nftorder.core.converter

import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.NftItemDto
import com.rarible.protocol.nftorder.core.model.Item
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component
import java.math.BigInteger

@Component
object NftItemDtoConverter : Converter<NftItemDto, Item> {

    override fun convert(nftItem: NftItemDto): Item {
        return Item(
            token = nftItem.contract,
            tokenId = EthUInt256(nftItem.tokenId),
            creators = PartDtoConverter.convert(nftItem.creators),
            supply = EthUInt256(nftItem.supply),
            lazySupply = EthUInt256(nftItem.lazySupply),
            royalties = PartDtoConverter.convert(nftItem.royalties),
            owners = nftItem.owners,
            date = nftItem.date ?: nowMillis(),
            pending = ItemTransferDtoConverter.convert(nftItem.pending ?: emptyList()),
            // Default enrichment data, should be replaced out of this converter
            sellers = 0,
            totalStock = BigInteger.ZERO,
            bestSellOrder = null,
            bestBidOrder = null,
            unlockable = false
        )
    }
}
