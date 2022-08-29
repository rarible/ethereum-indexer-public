package com.rarible.protocol.order.core.model

import io.daonomic.rpc.domain.Binary

enum class Platform(val id: Binary) {
    RARIBLE(id32("RARIBLE")),
    OPEN_SEA(id32("OPEN_SEA")),
    CRYPTO_PUNKS(id32("CRYPTO_PUNKS")),
    LOOKSRARE(id32("LOOKSRARE")),
    X2Y2(id32("X2Y2")),
    SUDOSWAP(id32("SUDOSWAP")),
}
