package com.rarible.protocol.order.listener.service.looksrare

import com.rarible.protocol.order.core.model.LooksrareV2Cursor

data class Result(
    val cursor: LooksrareV2Cursor?,
    val saved: Long
)
