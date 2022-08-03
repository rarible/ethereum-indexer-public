package com.rarible.protocol.order.core.misc

import com.rarible.protocol.dto.*

val OrderDto.platform: PlatformDto
    get() {
        return when (this) {
            is LegacyOrderDto, is RaribleV2OrderDto -> PlatformDto.RARIBLE
            is OpenSeaV1OrderDto, is SeaportV1OrderDto -> PlatformDto.OPEN_SEA
            is CryptoPunkOrderDto -> PlatformDto.CRYPTO_PUNKS
            is X2Y2OrderDto -> PlatformDto.X2Y2
            is LooksrareOrderDto -> PlatformDto.LOOKSRARE
        }
    }
