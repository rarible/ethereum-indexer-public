package com.rarible.protocol.order.core.converters.dto

import com.rarible.protocol.dto.OrderBasicSeaportDataV1Dto
import com.rarible.protocol.dto.OrderSeaportDataV1Dto
import com.rarible.protocol.order.core.model.OrderBasicSeaportDataV1
import com.rarible.protocol.order.core.model.OrderSeaportDataV1

object SeaportDataV1DtoConverter {
    fun convert(source: OrderSeaportDataV1): OrderSeaportDataV1Dto {
        return when (source) {
            is OrderBasicSeaportDataV1 -> OrderBasicSeaportDataV1Dto(
                protocol = source.protocol,
                orderType = SeaportOrderTypeDtoConverter.convert(source.orderType),
                offer = source.offer.map { SeaportOfferDtoConverter.convert(it) },
                consideration = source.consideration.map { SeaportConsiderationDtoConverter.convert(it) },
                zone = source.zone,
                zoneHash = source.zoneHash,
                conduitKey = source.conduitKey,
                counter = source.getCounterValue().value.toLong(), // TODO any ideas?
                nonce = source.getCounterValue().value
            )
        }
    }
}
