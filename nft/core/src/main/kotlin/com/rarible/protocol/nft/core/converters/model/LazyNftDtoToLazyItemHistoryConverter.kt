package com.rarible.protocol.nft.core.converters.model

import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.LazyErc1155Dto
import com.rarible.protocol.dto.LazyErc721Dto
import com.rarible.protocol.dto.LazyNftDto
import com.rarible.protocol.nft.core.model.ItemLazyMint
import com.rarible.protocol.nft.core.model.TokenStandard
import java.math.BigInteger

object LazyNftDtoToLazyItemHistoryConverter {

    fun convert(source: LazyNftDto): ItemLazyMint {
        val (value, standard) = when (source) {
            is LazyErc721Dto -> BigInteger.ONE to TokenStandard.ERC721
            is LazyErc1155Dto -> source.supply to TokenStandard.ERC1155
        }
        return ItemLazyMint(
            token = source.contract,
            tokenId = EthUInt256(source.tokenId),
            value = EthUInt256(value),
            standard = standard,
            date = nowMillis(),
            uri = source.uri,
            creators = source.creators.map { RoyaltyConverter.convert(it) },
            signatures = source.signatures,
            royalties = source.royalties.map { RoyaltyConverter.convert(it) }
        )
    }
}
