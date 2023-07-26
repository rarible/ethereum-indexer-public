package com.rarible.protocol.order.api.misc

import com.rarible.protocol.dto.LegacyOrderFormDto
import com.rarible.protocol.dto.OrderDataDto
import com.rarible.protocol.dto.OrderFormDto
import com.rarible.protocol.dto.RaribleV2OrderFormDto

val OrderFormDto.data: OrderDataDto
    get() = when (this) {
        is LegacyOrderFormDto -> data
        is RaribleV2OrderFormDto -> data
    }
