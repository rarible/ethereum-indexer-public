package com.rarible.protocol.order.core.model

enum class HistorySource {
    RARIBLE,
    OPEN_SEA,
    CRYPTO_PUNKS,
    X2Y2,
    LOOKSRARE,
    SUDOSWAP,
    BLUR,
    CMP,
    ;

    fun toPlatform(): Platform {
        return when (this) {
            RARIBLE -> Platform.RARIBLE
            CMP -> Platform.CMP
            OPEN_SEA -> Platform.OPEN_SEA
            CRYPTO_PUNKS -> Platform.CRYPTO_PUNKS
            X2Y2 -> Platform.X2Y2
            LOOKSRARE -> Platform.LOOKSRARE
            SUDOSWAP -> Platform.SUDOSWAP
            BLUR -> Platform.BLUR
        }
    }
}
