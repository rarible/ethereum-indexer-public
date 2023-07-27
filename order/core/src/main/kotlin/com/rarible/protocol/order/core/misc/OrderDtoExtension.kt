package com.rarible.protocol.order.core.misc

import com.rarible.protocol.dto.AmmOrderDto
import com.rarible.protocol.dto.CryptoPunkOrderDto
import com.rarible.protocol.dto.LegacyOrderDto
import com.rarible.protocol.dto.LooksRareOrderDto
import com.rarible.protocol.dto.LooksRareV2OrderDto
import com.rarible.protocol.dto.OpenSeaV1OrderDto
import com.rarible.protocol.dto.OrderDto
import com.rarible.protocol.dto.OrderSudoSwapAmmDataV1Dto
import com.rarible.protocol.dto.PlatformDto
import com.rarible.protocol.dto.RaribleV2OrderDto
import com.rarible.protocol.dto.SeaportV1OrderDto
import com.rarible.protocol.dto.X2Y2OrderDto

val OrderDto.platform: PlatformDto
    get() {
        return when (this) {
            is LegacyOrderDto, is RaribleV2OrderDto -> PlatformDto.RARIBLE
            is OpenSeaV1OrderDto, is SeaportV1OrderDto -> PlatformDto.OPEN_SEA
            is CryptoPunkOrderDto -> PlatformDto.CRYPTO_PUNKS
            is X2Y2OrderDto -> PlatformDto.X2Y2
            is LooksRareOrderDto -> PlatformDto.LOOKSRARE
            is LooksRareV2OrderDto -> PlatformDto.LOOKSRARE
            is AmmOrderDto -> when (data) {
                is OrderSudoSwapAmmDataV1Dto -> PlatformDto.SUDOSWAP
            }
        }
    }
