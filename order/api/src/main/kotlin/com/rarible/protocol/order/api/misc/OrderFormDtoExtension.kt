package com.rarible.protocol.order.api.misc

import com.rarible.protocol.dto.*

val OrderFormDto.data: OrderDataDto
    get() = when (this) {
        is LegacyOrderFormDto -> data
        is RaribleV2OrderFormDto -> data
    }
