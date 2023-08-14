package com.rarible.protocol.order.listener.service.order

import com.rarible.core.common.EventTimeMarks
import com.rarible.core.common.asyncWithTraceId
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.NftItemDto
import com.rarible.protocol.dto.NftItemUpdateEventDto
import com.rarible.protocol.dto.toModel
import com.rarible.protocol.order.core.misc.addIndexerIn
import com.rarible.protocol.order.core.misc.orderOffchainEventMarks
import com.rarible.protocol.order.core.model.ItemId
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.OrderCancelService
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class OrderItemService(
    private val orderRepository: OrderRepository,
    private val orderCancelService: OrderCancelService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun onItemChanged(event: NftItemUpdateEventDto) {
        val eventTimeMarks = event.eventTimeMarks?.toModel()?.addIndexerIn() ?: run {
            logger.warn("EventTimeMarks not found in NftItemUpdateEventDto")
            orderOffchainEventMarks()
        }
        onItemChanged(event.item, eventTimeMarks)
    }

    suspend fun onItemChanged(
        item: NftItemDto,
        eventTimeMarks: EventTimeMarks
    ) {
        if (item.isSuspiciousOnOS == true) {
            onOsSuspiciousItem(ItemId(item.contract, item.tokenId), eventTimeMarks)
        }
    }

    private suspend fun onOsSuspiciousItem(itemId: ItemId, eventTimeMarks: EventTimeMarks) {
        val start = System.currentTimeMillis()
        val toCancel = orderRepository.findSellOrdersNotCancelledByItemId(
            Platform.OPEN_SEA,
            itemId.contract,
            EthUInt256.of(itemId.tokenId)
        ).map { it.hash }.toList()

        if (toCancel.isEmpty()) {
            return
        }
        coroutineScope {
            toCancel.chunked(100).forEach { chunk ->
                chunk.map { hash ->
                    asyncWithTraceId(context = NonCancellable) {
                        logger.info("Cancelled Order {} for Item {} marked as suspicious", hash.prefixed(), itemId)
                        orderCancelService.cancelOrder(hash, eventTimeMarks)
                    }
                }.awaitAll()
            }
        }
        logger.info(
            "Cancelled {} active Orders for Item {} marked as suspicious ({}ms)",
            toCancel.size,
            itemId,
            System.currentTimeMillis() - start
        )
    }
}
