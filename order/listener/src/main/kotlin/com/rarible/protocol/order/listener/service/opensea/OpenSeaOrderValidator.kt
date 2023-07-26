package com.rarible.protocol.order.listener.service.opensea

import com.rarible.protocol.order.core.model.OrderVersion

interface OpenSeaOrderValidator {
    fun validate(order: OrderVersion): Boolean
}
