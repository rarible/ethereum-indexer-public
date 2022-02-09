package com.rarible.protocol.order.api.service.nft

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.api.client.NftOwnershipControllerApi
import com.rarible.protocol.order.core.model.Item
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
@CaptureSpan(type = SpanType.EXT)
class NftAssetService(
    private val nftOwnership: NftOwnershipControllerApi
) {

    suspend fun getOwnerNftAssets(owner: Address): List<Item> {
        var continuation: String? = null
        val nftAsserts = mutableListOf<Item>()

        do {
            val ownerships = nftOwnership.getNftOwnershipsByOwner(owner.prefixed(), continuation, null).awaitFirst()

            nftAsserts.addAll(ownerships.ownerships.map { item ->
                Item(
                    item.contract,
                    EthUInt256.Companion.of(item.tokenId)
                )
            })

            continuation = ownerships.continuation
        } while (continuation != null)

        return nftAsserts
    }
}
