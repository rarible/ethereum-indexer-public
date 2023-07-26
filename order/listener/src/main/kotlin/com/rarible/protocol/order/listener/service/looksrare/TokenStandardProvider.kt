package com.rarible.protocol.order.listener.service.looksrare

import com.rarible.protocol.dto.NftCollectionDto
import com.rarible.protocol.order.core.model.TokenStandard
import com.rarible.protocol.order.core.service.nft.NftCollectionApiService
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class TokenStandardProvider(
    private val nftCollectionApiService: NftCollectionApiService
) {
    suspend fun getTokenStandard(token: Address): TokenStandard? {
        val collection = nftCollectionApiService.getNftCollectionById(token) ?: return null
        return when (collection.type) {
            NftCollectionDto.Type.ERC721,
            NftCollectionDto.Type.CRYPTO_PUNKS -> TokenStandard.ERC721
            NftCollectionDto.Type.ERC1155 -> TokenStandard.ERC1155
        }
    }
}
