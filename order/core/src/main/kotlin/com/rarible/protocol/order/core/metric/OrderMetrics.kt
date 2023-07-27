package com.rarible.protocol.order.core.metric

import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.order.core.model.Platform
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

@Component
class OrderMetrics(
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    private val blockchain: Blockchain,
    meterRegistry: MeterRegistry,
) : BaseOrderMetrics(meterRegistry) {

    fun onOrderExecution(platform: Platform) {
        increment(ORDER_EXECUTION, tag(blockchain), tag(platform))
    }

    fun onOrderExecutionFailed(platform: Platform, error: ExecutionError) {
        increment(ORDER_EXECUTION_FAILED, tag(blockchain), tag(platform), tag("error", error.value))
    }

    private companion object {
        const val ORDER_EXECUTION_FAILED = "order_execution_failed"
        const val ORDER_EXECUTION = "order_execution"
    }
}
