package com.rarible.protocol.order.core.model

import io.daonomic.rpc.domain.Binary

data class ApplyReplacementPatternResult(
    val callData1: Binary,
    val callData2: Binary
) {
    fun isValid(): Boolean {
        return callData1 == callData2
    }
}
