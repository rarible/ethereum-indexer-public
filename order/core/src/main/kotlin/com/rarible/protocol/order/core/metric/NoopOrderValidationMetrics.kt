package com.rarible.protocol.order.core.metric

import com.rarible.protocol.order.core.model.Platform
import org.springframework.stereotype.Component

@Component
class NoopOrderValidationMetrics : OrderValidationMetrics {
    override fun onOrderValidationSuccess(platform: Platform, type: String) {
    }

    override fun onOrderValidationFail(platform: Platform, type: String) {
    }
}
