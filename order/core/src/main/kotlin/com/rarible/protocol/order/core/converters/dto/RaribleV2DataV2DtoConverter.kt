package com.rarible.protocol.order.core.converters.dto

import com.rarible.protocol.dto.OrderRaribleV2DataV2Dto
import com.rarible.protocol.dto.PartDto
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV2
import com.rarible.protocol.order.core.model.Part
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
object RaribleV2DataV2DtoConverter : Converter<OrderRaribleV2DataV2, OrderRaribleV2DataV2Dto> {
    override fun convert(source: OrderRaribleV2DataV2): OrderRaribleV2DataV2Dto {
        val payouts = convert(source.payouts)
        val originFees = convert(source.originFees)
        return OrderRaribleV2DataV2Dto(payouts = payouts, originFees = originFees, isMakeFill = source.isMakeFill)
    }

    private fun convert(source: List<Part>): List<PartDto> {
        return source.map { PartDto(it.account, it.value.value.intValueExact()) }
    }
}
