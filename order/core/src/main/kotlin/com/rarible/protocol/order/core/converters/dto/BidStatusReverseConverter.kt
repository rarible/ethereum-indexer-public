package com.rarible.protocol.order.core.converters.dto

import com.rarible.protocol.dto.OrderStatusDto
import com.rarible.protocol.order.core.model.BidStatus
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
object BidStatusReverseConverter : Converter<OrderStatusDto, BidStatus> {
    override fun convert(source: OrderStatusDto): BidStatus {
        return when (source) {
            OrderStatusDto.ACTIVE -> BidStatus.ACTIVE
            OrderStatusDto.FILLED -> BidStatus.FILLED
            OrderStatusDto.HISTORICAL -> BidStatus.HISTORICAL
            OrderStatusDto.INACTIVE -> BidStatus.INACTIVE
            OrderStatusDto.CANCELLED -> BidStatus.CANCELLED
        }
    }
}
