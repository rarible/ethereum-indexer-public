package com.rarible.protocol.order.core.converters.dto

import com.rarible.protocol.dto.AuctionStatusDto
import com.rarible.protocol.order.core.model.AuctionStatus

object AuctionStatusDtoConverter {
    fun convert(source: AuctionStatus): AuctionStatusDto {
        return when (source) {
            AuctionStatus.ACTIVE -> AuctionStatusDto.ACTIVE
            AuctionStatus.CANCELLED -> AuctionStatusDto.CANCELLED
            AuctionStatus.FINISHED -> AuctionStatusDto.FINISHED
        }
    }
}
