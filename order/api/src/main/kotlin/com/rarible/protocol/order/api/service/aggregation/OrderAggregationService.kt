package com.rarible.protocol.order.api.service.aggregation

import com.rarible.protocol.order.api.exceptions.ValidationApiException
import com.rarible.protocol.order.core.model.AggregatedData
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepository
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import java.util.*

@Component
class OrderAggregationService(
    private val exchangeHistoryRepository: ExchangeHistoryRepository
) {
    fun getNftSellOrdersAggregationByMaker(startDate: Date, endDate: Date, source: HistorySource?): Flux<AggregatedData> {
        validateDates(startDate, endDate)
        return exchangeHistoryRepository.getMakerNftSellAggregation(startDate, endDate, source)
    }

    fun getNftPurchaseOrdersAggregationByTaker(startDate: Date, endDate: Date, source: HistorySource?): Flux<AggregatedData> {
        validateDates(startDate, endDate)
        return exchangeHistoryRepository.getTakerNftBuyAggregation(startDate, endDate, source)
    }

    fun getNftPurchaseOrdersAggregationByCollection(startDate: Date, endDate: Date, source: HistorySource?): Flux<AggregatedData> {
        validateDates(startDate, endDate)
        return exchangeHistoryRepository.getTokenPurchaseAggregation(startDate, endDate, source)
    }

    private fun validateDates(startDate: Date, endDate: Date) {
        if (startDate >= endDate) {
            throw ValidationApiException("startDate must be greater then endDate")
        }
    }
}
