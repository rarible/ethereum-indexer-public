package com.rarible.protocol.order.listener.service.order

import com.rarible.core.common.EventTimeMarks
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
