package com.rarible.protocol.order.api.converter

import com.rarible.protocol.dto.OrderFormDto
import com.rarible.protocol.order.core.model.Platform

object OrderFormPlatformConverter {
    fun convert(source: OrderFormDto.Platform?): Platform {
        return when (source) {
            OrderFormDto.Platform.CMP -> Platform.CMP
            OrderFormDto.Platform.RARIBLE, null -> Platform.RARIBLE
        }
    }
}