package com.rarible.protocol.order.core.converters.dto

import com.rarible.protocol.dto.OrderEventDto
import com.rarible.protocol.dto.OrderUpdateEventDto
import com.rarible.protocol.order.core.model.OrderEvent
import com.rarible.protocol.order.core.model.OrderUpdateEvent
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
object OrderEventDtoConverter : Converter<OrderEvent, Array<OrderEventDto>> {
    override fun convert(source: OrderEvent): Array<OrderEventDto> {
        require(source.subEvents.size <= 1) {
            "Order converter can handle only single or zero event, but has ${source.subEvents.size}"
        }
        return source.subEvents.map {
            when (it) {
                is OrderUpdateEvent -> OrderUpdateEventDto(
                    eventId = source.id,
                    orderId = source.entityId,
                    order = OrderDtoConverter.convert(it.order)
                )
            }
        }.toTypedArray()
    }
}
