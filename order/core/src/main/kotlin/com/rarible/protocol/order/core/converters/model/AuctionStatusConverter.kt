package com.rarible.protocol.order.core.converters.model

import com.rarible.protocol.dto.AuctionStatusDto
import com.rarible.protocol.order.core.model.AuctionStatus

object AuctionStatusConverter {
    fun convert(source: AuctionStatusDto): AuctionStatus {
        return when (source) {
            AuctionStatusDto.ACTIVE -> AuctionStatus.ACTIVE
            AuctionStatusDto.CANCELLED -> AuctionStatus.CANCELLED
            AuctionStatusDto.FINISHED -> AuctionStatus.FINISHED
        }
    }
}
