package com.rarible.protocol.order.core.converters.dto

import com.rarible.protocol.dto.AggregationDataDto
import com.rarible.protocol.order.core.model.AggregatedData
import org.springframework.stereotype.Component

@Component
object AggregationDataDtoConverter {

    fun convert(source: AggregatedData): AggregationDataDto {
        return AggregationDataDto(
            address = source.address,
            sum = source.sum,
            count = source.count
        )
    }
}
