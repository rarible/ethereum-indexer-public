package com.rarible.protocol.order.listener.service.opensea

import com.rarible.opensea.client.model.v1.OpenSeaOrder
import java.time.Duration

interface OpenSeaOrderService {

    suspend fun getNextOrdersBatch(
        listedAfter: Long,
        listedBefore: Long,
        loadPeriod: Duration,
        logPrefix: String
    ): List<OpenSeaOrder>
}
