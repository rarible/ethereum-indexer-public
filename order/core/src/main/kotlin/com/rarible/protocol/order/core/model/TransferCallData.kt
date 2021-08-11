package com.rarible.protocol.order.core.model

import io.daonomic.rpc.domain.Binary

data class TransferCallData(
    val callData: Binary,
    val replacementPattern: Binary
)
