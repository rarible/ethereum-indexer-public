package com.rarible.protocol.order.core.event

import com.rarible.protocol.order.core.converters.dto.OrderActivityConverter
import com.rarible.protocol.order.core.model.ActivityResult
import com.rarible.protocol.order.core.model.OrderActivityResult
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.producer.ProtocolOrderPublisher
import org.springframework.stereotype.Component

@Component
class OrderVersionListener(
    private val orderActivityConverter: OrderActivityConverter,
    private val eventPublisher: ProtocolOrderPublisher
) {
    suspend fun onOrderVersion(orderVersion: OrderVersion) {
        orderActivityConverter
            .convert(OrderActivityResult.Version(orderVersion))
            ?.let { eventPublisher.publish(it) }
    }
}
