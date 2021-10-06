package com.rarible.protocol.order.core.converters.dto

import com.rarible.protocol.dto.OrderStatusDto
import com.rarible.protocol.order.core.model.BidStatus
import org.springframework.core.convert.converter.Converter

object BidStatusConverter : Converter<BidStatus, OrderStatusDto> {
    override fun convert(source: BidStatus): OrderStatusDto {
        return when (source) {
            BidStatus.ACTIVE -> OrderStatusDto.ACTIVE
            BidStatus.FILLED -> OrderStatusDto.FILLED
            BidStatus.HISTORICAL -> OrderStatusDto.HISTORICAL
            BidStatus.INACTIVE -> OrderStatusDto.INACTIVE
            BidStatus.CANCELLED -> OrderStatusDto.CANCELLED
        }
    }
}
