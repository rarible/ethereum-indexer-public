package com.rarible.protocol.order.core.metric

import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.Platform
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

@Component
class OrderValidationMetrics(
    properties: OrderIndexerProperties,
    meterRegistry: MeterRegistry
) : BaseOrderMetrics(meterRegistry) {

    private val blockchain = properties.blockchain

    fun onOrderValidationSuccess(platform: Platform, type: String) {
        onOrderValidation(platform, type, "ok")
    }

    fun onOrderValidationFail(platform: Platform, type: String) {
        onOrderValidation(platform, type, "fail")
    }

    private fun onOrderValidation(
        platform: Platform,
        type: String,
        status: String
    ) {
        meterRegistry.counter(
            ORDER_VALIDATION,
            listOf(
                tag(blockchain),
                tag(platform),
                type(type.lowercase()),
                status(status.lowercase())
            )
        ).increment()
    }

    private companion object {

        const val ORDER_VALIDATION = "order_validation"
    }
}
