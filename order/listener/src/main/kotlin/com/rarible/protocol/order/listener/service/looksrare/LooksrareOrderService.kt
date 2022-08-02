package com.rarible.protocol.order.listener.service.looksrare

import com.rarible.looksrare.client.model.v1.LooksRareOrder
import com.rarible.looksrare.client.model.v1.LooksRareOrders
import java.time.Duration

interface LooksrareOrderService {

    suspend fun getNextSellOrders(nextCursor: String?): LooksRareOrders

    suspend fun getNextOrdersBatch(
        listedAfter: Long,
        listedBefore: Long,
        loadPeriod: Duration,
        logPrefix: String
    ): List<LooksRareOrder>
}
