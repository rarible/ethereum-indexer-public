package com.rarible.protocol.order.core.metric

import com.rarible.protocol.order.core.model.Platform

interface OrderValidationMetrics {
    fun onOrderValidationSuccess(platform: Platform, type: String)
    fun onOrderValidationFail(platform: Platform, type: String)
}
