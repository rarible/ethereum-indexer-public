package com.rarible.protocol.order.core.converters.dto

import com.rarible.protocol.dto.OrderRaribleV2DataV1Dto
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV1
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
object RaribleV2DataV1DtoConverter : Converter<OrderRaribleV2DataV1, OrderRaribleV2DataV1Dto> {
    override fun convert(source: OrderRaribleV2DataV1): OrderRaribleV2DataV1Dto {
        val payouts = PartListDtoConverter.convert(source.payouts)
        val originFees = PartListDtoConverter.convert(source.originFees)
        return OrderRaribleV2DataV1Dto(payouts = payouts, originFees = originFees)
    }
}
