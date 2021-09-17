package com.rarible.protocol.order.core.model

data class FeatureFlags(
    val useCommonTransactionTraceProvider: Boolean = true,
    val showAllOrdersByDefault: Boolean = false
)
