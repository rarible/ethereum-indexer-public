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
import kotlinx.coroutines.flow.take
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
        val toCancel = orderRepository.findNonTerminateStatusSellOrdersByItemId(
            Platform.OPEN_SEA,
            itemId.contract,
            EthUInt256.of(itemId.tokenId)
        ).take(ORDER_TO_CANCEL_MAX_COUNT).map { it.hash }.toList()

        // Should never happen since there are not a lot of such items/orders, but let's monitor it
        if (toCancel.size == ORDER_TO_CANCEL_MAX_COUNT) {
            logger.warn("WARNING! Too many active Orders found for Item {} marked as suspicious", itemId)
        }

        if (toCancel.isEmpty()) {
            return
        }
        coroutineScope {
            toCancel.chunked(ORDER_TO_CANCEL_CHUNK_SIZE).forEach { chunk ->
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

    companion object {
        const val ORDER_TO_CANCEL_MAX_COUNT = 1000
        const val ORDER_TO_CANCEL_CHUNK_SIZE = 100
    }
}
