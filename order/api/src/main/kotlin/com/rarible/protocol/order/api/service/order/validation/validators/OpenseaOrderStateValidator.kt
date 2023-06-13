package com.rarible.protocol.order.api.service.order.validation.validators

import com.rarible.protocol.order.api.service.order.signature.OrderSignatureResolver
import com.rarible.protocol.order.api.service.order.validation.OrderStateValidator
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderDataVersion
import com.rarible.protocol.order.core.model.Platform
import org.springframework.stereotype.Component

@Component
class OpenseaOrderStateValidator(
    private val orderSignatureResolver: OrderSignatureResolver,
) : OrderStateValidator {
    override suspend fun validate(order: Order) {
        if (order.platform == Platform.OPEN_SEA && order.data.version == OrderDataVersion.BASIC_SEAPORT_DATA_V1) {
            orderSignatureResolver.resolveSeaportSignature(order.hash)
        }
    }
}