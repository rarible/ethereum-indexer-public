package com.rarible.protocol.order.core.converters.dto

import com.rarible.protocol.dto.OrderStatusDto
import com.rarible.protocol.order.core.model.OrderStatus
import org.springframework.core.convert.converter.Converter

object OrderStatusDtoConverter : Converter<OrderStatus, OrderStatusDto> {
    override fun convert(source: OrderStatus): OrderStatusDto {
        return when (source) {
            OrderStatus.ACTIVE -> OrderStatusDto.ACTIVE
            OrderStatus.FILLED -> OrderStatusDto.FILLED
            OrderStatus.INACTIVE -> OrderStatusDto.INACTIVE
            OrderStatus.CANCELLED -> OrderStatusDto.CANCELLED
        }
    }
}
