package com.rarible.protocol.order.listener.service.opensea

import com.rarible.opensea.client.model.v2.SeaportOrders

interface OpenSeaOrderService {
    suspend fun getNextSellOrders(nextCursor: String?): SeaportOrders
}
