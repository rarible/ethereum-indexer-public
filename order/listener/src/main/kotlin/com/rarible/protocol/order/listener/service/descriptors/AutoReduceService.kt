package com.rarible.protocol.order.listener.service.descriptors

import com.rarible.blockchain.scanner.ethereum.model.EthereumLogRecord
import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.protocol.order.core.model.AuctionHistory
import com.rarible.protocol.order.core.model.AutoReduce
import com.rarible.protocol.order.core.model.OrderHistory
import com.rarible.protocol.order.core.repository.AutoReduceRepository
import org.springframework.stereotype.Service

@Service
class AutoReduceService(
    private val autoReduceRepository: AutoReduceRepository
) {

    suspend fun autoReduce(logs: List<EthereumLogRecord>) {
        val data = logs.filterIsInstance(ReversedEthereumLogRecord::class.java).map { it.data }
        val auctionIds = data.filterIsInstance(AuctionHistory::class.java).map {
            AutoReduce(it.hash.toString())
        }.toSet()
        val orderIds = data.filterIsInstance(OrderHistory::class.java).map {
            AutoReduce(it.hash.toString())
        }.toSet()
        if (auctionIds.isNotEmpty()) {
            autoReduceRepository.saveAuctions(auctionIds)
        }
        if (orderIds.isNotEmpty()) {
            autoReduceRepository.saveOrders(orderIds)
        }
    }
}
