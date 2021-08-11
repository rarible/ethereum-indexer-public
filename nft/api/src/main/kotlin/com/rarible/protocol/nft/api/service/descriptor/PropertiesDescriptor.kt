package com.rarible.protocol.nft.api.service.descriptor

import com.rarible.contracts.erc1155.IERC1155
import com.rarible.contracts.erc721.IERC721
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.TokenStandard
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.stereotype.Component
import scalether.domain.Address
import scalether.transaction.MonoTransactionSender
import java.net.URI

@Component
class PropertiesDescriptor(
    private val sender: MonoTransactionSender
) {
   suspend fun getTokenUri(token: Address, tokenId: EthUInt256, standard: TokenStandard): URI? {
       val uri = when (standard) {
           TokenStandard.ERC1155 -> IERC1155(token, sender).uri(tokenId.value)
           else -> IERC721(token, sender).tokenURI(tokenId.value)
       }
       return uri.awaitFirstOrNull()?.let { URI.create(it) }
   }
}