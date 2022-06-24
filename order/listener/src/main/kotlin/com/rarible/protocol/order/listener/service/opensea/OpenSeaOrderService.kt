package com.rarible.protocol.order.listener.service.opensea

import com.rarible.opensea.client.model.v1.OpenSeaOrder
import com.rarible.opensea.client.model.v2.SeaportOrder
import com.rarible.opensea.client.model.v2.SeaportOrders
import java.time.Duration

interface OpenSeaOrderService {

    suspend fun getNextSellOrders(nextCursor: String? = null): SeaportOrders

    suspend fun getNextOrdersBatch(
        listedAfter: Long,
        listedBefore: Long,
        loadPeriod: Duration,
        logPrefix: String
    ): List<OpenSeaOrder>
}
