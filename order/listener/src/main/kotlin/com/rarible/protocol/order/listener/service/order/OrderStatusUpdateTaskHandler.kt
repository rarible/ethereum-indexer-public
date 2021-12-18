package com.rarible.protocol.order.listener.service.order

import com.rarible.core.task.TaskHandler
import com.rarible.protocol.dto.OrderUpdateEventDto
import com.rarible.protocol.order.core.converters.dto.OrderDtoConverter
import com.rarible.protocol.order.core.model.OrderStatus
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.producer.ProtocolOrderPublisher
import com.rarible.protocol.order.core.repository.order.OrderRepository
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Component
import java.util.*

@Component
class OrderStatusUpdateTaskHandler(
    private val orderRepository: OrderRepository,
    private val publisher: ProtocolOrderPublisher,
    private val orderDtoConverter: OrderDtoConverter
) : TaskHandler<String> {

    override val type: String
        get() = "ORDER_STATUS_UPDATE"

    override fun runLongTask(from: String?, param: String): Flow<String> {
        val (platform, orderStatus) = parseParameter(param)
        return orderRepository.findAll(platform, orderStatus, from?.let { Word.apply(it) }).map { order ->
            val newOrder = order.withUpdatedStatus()
            orderRepository.save(newOrder)
            val updateEvent = OrderUpdateEventDto(
                eventId = UUID.randomUUID().toString(),
                orderId = order.hash.toString(),
                order = orderDtoConverter.convert(order)
            )
            publisher.publish(updateEvent)
            order.hash.toString()
        }
    }

    companion object {
        fun parseParameter(param: String): Pair<Platform, OrderStatus> {
            val platform = Platform.valueOf(param.substringBefore(":"))
            val orderStatus = OrderStatus.valueOf(param.substringAfter(":"))
            return platform to orderStatus
        }
    }
}
