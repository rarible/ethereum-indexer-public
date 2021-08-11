package com.rarible.protocol.order.core.converters.model

import com.rarible.protocol.dto.OrderBidStatusDto
import com.rarible.protocol.order.core.model.BidStatus
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
object OrderBidStatusConverter : Converter<OrderBidStatusDto, BidStatus> {
    override fun convert(source: OrderBidStatusDto): BidStatus {
        return when (source) {
            OrderBidStatusDto.ACTIVE -> BidStatus.ACTIVE
            OrderBidStatusDto.HISTORICAL -> BidStatus.HISTORICAL
            OrderBidStatusDto.INACTIVE -> BidStatus.INACTIVE
            OrderBidStatusDto.CANCELLED -> BidStatus.CANCELLED
            OrderBidStatusDto.FILLED -> BidStatus.FILLED
        }
    }
}
