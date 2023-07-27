package com.rarible.protocol.order.core.validator

import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderDataVersion
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.service.SeaportSignatureResolver
import org.springframework.stereotype.Component

@Component
class OpenseaOrderStateValidator(
    private val seaportSignatureResolver: SeaportSignatureResolver,
) : OrderStateValidator {

    override val type = "aggregation"

    override fun supportsValidation(order: Order): Boolean {
        return order.platform == Platform.OPEN_SEA &&
            order.data.version == OrderDataVersion.BASIC_SEAPORT_DATA_V1
    }

    override suspend fun validate(order: Order) {
        seaportSignatureResolver.resolveSeaportSignature(order.hash)
    }
}
