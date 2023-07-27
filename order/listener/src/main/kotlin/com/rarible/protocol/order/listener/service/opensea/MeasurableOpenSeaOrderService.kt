package com.rarible.protocol.order.listener.service.opensea

import com.rarible.opensea.client.model.v2.SeaportOrders
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.listener.misc.ForeignOrderMetrics

class MeasurableOpenSeaOrderService(
    private val delegate: OpenSeaOrderService,
    private val metrics: ForeignOrderMetrics
) : OpenSeaOrderService {

    override suspend fun getNextSellOrders(nextCursor: String?, loadAhead: Boolean): SeaportOrders {
        val orders = delegate.getNextSellOrders(nextCursor, loadAhead)
        orders.orders.maxOfOrNull { it.createdAt }?.let {
            metrics.onLatestOrderReceived(Platform.OPEN_SEA, it)
        }
        return orders
    }
}
