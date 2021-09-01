package com.rarible.protocol.order.listener.service.opensea

import com.rarible.opensea.client.model.OpenSeaOrder

interface OpenSeaOrderService {
    suspend fun getNextOrdersBatch(listedAfter: Long, listedBefore: Long): List<OpenSeaOrder>
}
