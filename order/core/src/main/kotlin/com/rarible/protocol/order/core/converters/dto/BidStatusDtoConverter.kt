package com.rarible.protocol.order.core.converters.dto

import com.rarible.protocol.dto.OrderBidStatusDto
import com.rarible.protocol.order.core.model.BidStatus
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
object BidStatusDtoConverter : Converter<BidStatus, OrderBidStatusDto> {
    override fun convert(source: BidStatus): OrderBidStatusDto {
        return when (source) {
            BidStatus.ACTIVE -> OrderBidStatusDto.ACTIVE
            BidStatus.FILLED -> OrderBidStatusDto.FILLED
            BidStatus.HISTORICAL -> OrderBidStatusDto.HISTORICAL
            BidStatus.INACTIVE -> OrderBidStatusDto.INACTIVE
            BidStatus.CANCELLED -> OrderBidStatusDto.CANCELLED
        }
    }
}
