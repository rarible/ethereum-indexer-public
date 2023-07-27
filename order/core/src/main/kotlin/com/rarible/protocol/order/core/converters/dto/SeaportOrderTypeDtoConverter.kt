package com.rarible.protocol.order.core.converters.dto

import com.rarible.protocol.dto.SeaportOrderTypeDto
import com.rarible.protocol.order.core.model.SeaportOrderType

object SeaportOrderTypeDtoConverter {
    fun convert(source: SeaportOrderType): SeaportOrderTypeDto {
        return when (source) {
            SeaportOrderType.FULL_OPEN -> SeaportOrderTypeDto.FULL_OPEN
            SeaportOrderType.PARTIAL_OPEN -> SeaportOrderTypeDto.PARTIAL_OPEN
            SeaportOrderType.FULL_RESTRICTED -> SeaportOrderTypeDto.FULL_RESTRICTED
            SeaportOrderType.PARTIAL_RESTRICTED -> SeaportOrderTypeDto.PARTIAL_RESTRICTED
            SeaportOrderType.CONTRACT -> SeaportOrderTypeDto.CONTRACT
        }
    }
}
