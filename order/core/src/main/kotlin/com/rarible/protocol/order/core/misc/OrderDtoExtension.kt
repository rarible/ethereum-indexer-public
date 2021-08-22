package com.rarible.protocol.order.core.misc

import com.rarible.protocol.dto.*

val OrderDto.platform: PlatformDto
    get() {
        return when (this) {
            is LegacyOrderDto, is RaribleV2OrderDto -> PlatformDto.RARIBLE
            is OpenSeaV1OrderDto -> PlatformDto.OPEN_SEA
            is CryptoPunkOrderDto -> PlatformDto.CRYPTO_PUNKS
        }
    }
