package com.rarible.protocol.order.core.metric

import com.rarible.ethereum.domain.Blockchain
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

@Component
class FloorOrderCheckMetrics(
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    private val blockchain: Blockchain,
    meterRegistry: MeterRegistry,
) : BaseOrderMetrics(meterRegistry) {

    fun onOrderChecked() {
        increment(FLOOR_ORDER_CHECKED, tag(blockchain))
    }

    fun onOrderSimulated(status: String) {
        increment(FLOOR_ORDER_SIMULATED, tag(blockchain), tag("status", status))
    }

    private companion object {
        const val FLOOR_ORDER_CHECKED = "floor_order_check"
        const val FLOOR_ORDER_SIMULATED = "floor_order_simulated"
    }
}
