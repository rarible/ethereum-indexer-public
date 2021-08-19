package com.rarible.protocol.order.core.converters.dto

import com.rarible.protocol.dto.OrderPriceHistoryRecordDto
import com.rarible.protocol.order.core.model.OrderPriceHistoryRecord
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
object OrderPriceHistoryDtoConverter : Converter<OrderPriceHistoryRecord, OrderPriceHistoryRecordDto> {
    override fun convert(source: OrderPriceHistoryRecord): OrderPriceHistoryRecordDto {
        return OrderPriceHistoryRecordDto(
            date = source.date,
            takeValue = source.takeValue,
            makeValue = source.makeValue
        )
    }

}
