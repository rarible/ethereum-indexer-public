package com.rarible.protocol.order.core.service.pool

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.model.ItemId
import com.rarible.protocol.order.core.model.PoolNftChange
import com.rarible.protocol.order.core.model.PoolNftIn
import com.rarible.protocol.order.core.model.PoolNftItemIds
import com.rarible.protocol.order.core.model.PoolNftOut
import com.rarible.protocol.order.core.repository.pool.PoolHistoryRepository
import com.rarible.protocol.order.core.service.nft.NftOwnershipApiService
import io.daonomic.rpc.domain.Word
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class PoolOwnershipService(
    private val nftOwnershipApiService: NftOwnershipApiService,
    private val poolHistoryRepository: PoolHistoryRepository
) {
    suspend fun getPoolHashesByItemId(contract: Address, tokenId: EthUInt256): List<Word> {
        return poolHistoryRepository
            .getLatestPoolNftChange(contract, tokenId)
            .mapNotNull {
                when (val data = it.data as PoolNftChange) {
                    is PoolNftIn -> data.hash
                    is PoolNftOut -> null
                }
            }
    }

    suspend fun getPoolItemIds(
        poolAddress: Address,
        collection: Address,
        continuation: String?,
        limit: Int
    ): PoolNftItemIds {
        val result = nftOwnershipApiService.getOwnershipsByOwnerAndCollection(
            poolAddress,
            collection,
            continuation,
            limit
        )
        return PoolNftItemIds(
            itemIds = result.ownerships.map { ItemId(it.contract, it.tokenId) },
            continuation = result.continuation
        )
    }
}
