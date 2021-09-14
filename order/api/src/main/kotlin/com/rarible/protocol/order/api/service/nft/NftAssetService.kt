package com.rarible.protocol.order.api.service.nft

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.api.client.NftItemControllerApi
import com.rarible.protocol.order.core.model.Item
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class NftAssetService(
    private val nftItemApi: NftItemControllerApi
) {
    suspend fun getOwnerNftAssets(owner: Address): List<Item> {
        var continuation: String? = null
        val nftAsserts = mutableListOf<Item>()

        do {
            val items = nftItemApi.getNftItemsByOwner(owner.hex(), continuation, null).awaitFirst()

            nftAsserts.addAll(items.items.map { item ->
                Item(
                    item.contract,
                    EthUInt256.Companion.of(item.tokenId)
                )
            })

            continuation = items.continuation
        } while (continuation != null)

        return nftAsserts
    }
}
