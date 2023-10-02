package com.rarible.protocol.nft.listener.service.subscribers

import com.rarible.blockchain.scanner.ethereum.model.EthereumLogRecord
import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.protocol.nft.core.model.AutoReduce
import com.rarible.protocol.nft.core.model.CollectionEvent
import com.rarible.protocol.nft.core.model.ItemHistory
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.repository.AutoReduceRepository
import org.springframework.stereotype.Service

@Service
class AutoReduceService(
    private val autoReduceRepository: AutoReduceRepository
) {

    suspend fun autoReduce(logs: List<EthereumLogRecord>) {
        val data = logs.filterIsInstance(ReversedEthereumLogRecord::class.java).map { it.data }
        val itemIds = data.filterIsInstance(ItemHistory::class.java).map {
            AutoReduce(ItemId(it.token, it.tokenId).toString())
        }.toSet()
        val collectionIds = data.filterIsInstance(CollectionEvent::class.java).map {
            AutoReduce(it.id.toString())
        }.toSet()
        if (itemIds.isNotEmpty()) {
            autoReduceRepository.saveItems(itemIds)
        }
        if (collectionIds.isNotEmpty()) {
            autoReduceRepository.saveTokens(collectionIds)
        }
    }
}
