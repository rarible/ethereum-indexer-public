package com.rarible.protocol.order.listener.service.order

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.EventTimeMarksDto
import com.rarible.protocol.dto.NftItemDto
import com.rarible.protocol.dto.NftItemUpdateEventDto
import com.rarible.protocol.dto.add
import com.rarible.protocol.dto.offchainEventMark
import com.rarible.protocol.order.core.model.ItemId
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.OrderCancelService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class OrderItemService(
    private val orderRepository: OrderRepository,
    private val orderCancelService: OrderCancelService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun onItemChanged(event: NftItemUpdateEventDto) {
        onItemChanged(event.item, event.eventTimeMarks)
    }

    suspend fun onItemChanged(
        item: NftItemDto,
        eventTimeMarks: EventTimeMarksDto? = null
    ) {
        if (item.isSuspiciousOnOS == true) {
            val marks = "indexer-in_order".let { eventTimeMarks?.add(it) ?: offchainEventMark(it) }
            onOsSuspiciousItem(ItemId(item.contract, item.tokenId), marks)
        }
    }

    private suspend fun onOsSuspiciousItem(itemId: ItemId, eventTimeMarks: EventTimeMarksDto) {
        orderRepository.findSellOrdersNotCancelledByItemId(
            Platform.OPEN_SEA,
            itemId.contract,
            EthUInt256.of(itemId.tokenId)
        ).collect { order ->
            logger.info("Cancelled Order {} - Item {} marked as suspicious", order.hash.prefixed(), itemId)
            orderCancelService.cancelOrder(order.hash, eventTimeMarks)
        }
    }

}