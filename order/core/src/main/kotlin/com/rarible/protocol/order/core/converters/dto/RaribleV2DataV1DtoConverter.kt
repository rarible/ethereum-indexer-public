package com.rarible.protocol.order.core.converters.dto

import com.rarible.protocol.dto.OrderRaribleV2DataV1Dto
import com.rarible.protocol.dto.PartDto
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV1
import com.rarible.protocol.order.core.model.Part
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
object RaribleV2DataV1DtoConverter : Converter<OrderRaribleV2DataV1, OrderRaribleV2DataV1Dto> {
    override fun convert(source: OrderRaribleV2DataV1): OrderRaribleV2DataV1Dto {
        val payouts = convert(source.payouts)
        val originFees = convert(source.originFees)
        return OrderRaribleV2DataV1Dto(payouts = payouts, originFees = originFees)
    }

    private fun convert(source: List<Part>): List<PartDto> {
        return source.map { PartDto(it.account, it.value.value.intValueExact()) }
    }
}
