package com.rarible.protocol.order.core.service.block.order

import com.rarible.blockchain.scanner.framework.data.LogRecordEvent
import com.rarible.blockchain.scanner.framework.listener.LogRecordEventSubscriber
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.misc.addIndexerIn
import com.rarible.protocol.order.core.misc.asEthereumLogRecord
import com.rarible.protocol.order.core.model.CryptoPunksAssetType
import com.rarible.protocol.order.core.model.MakeBalanceState
import com.rarible.protocol.order.core.model.OnChainOrder
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.OrderUpdateService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
@Qualifier("order-event-subscriber")
class CryptoPunkOnChainOrderEventSubscriber(
    private val orderRepository: OrderRepository,
    private val orderUpdateService: OrderUpdateService,
) : LogRecordEventSubscriber {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override suspend fun onLogRecordEvents(events: List<LogRecordEvent>) {
        events.asSequence().forEach { handleEvent(it) }
    }

    private suspend fun handleEvent(event: LogRecordEvent) {
        val record = event.record.asEthereumLogRecord()
        val onChainOrder = record.data as? OnChainOrder ?: return
        val type = onChainOrder.make.type as? CryptoPunksAssetType ?: return

        val eventTimeMarks = event.eventTimeMarks.addIndexerIn()

        val token = type.token
        val tokenId = type.tokenId
        orderRepository
            .findByTargetNftAndNotCanceled(onChainOrder.maker, token, tokenId)
            .collect {
                if (it.type == OrderType.RARIBLE_V2) {
                    orderUpdateService.updateMakeStock(
                        hash = it.hash,
                        makeBalanceState = MakeBalanceState(EthUInt256.ZERO, it.lastUpdateAt),
                        eventTimeMarks = eventTimeMarks
                    )
                } else {
                    logger.warn("Unexpected Order type in CryptoPunks order, RARIBLE_V2 expected: {}", it)
                }
            }
    }
}
