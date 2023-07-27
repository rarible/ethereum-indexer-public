package com.rarible.protocol.order.core.model

import java.math.BigDecimal

sealed class OrderUsdValue {
    abstract val makeUsd: BigDecimal?
    abstract val takeUsd: BigDecimal?
    abstract val takePriceUsd: BigDecimal?
    abstract val makePriceUsd: BigDecimal?

    data class BidOrder(
        override val makeUsd: BigDecimal,
        override val takePriceUsd: BigDecimal
    ) : OrderUsdValue() {
        override val makePriceUsd: BigDecimal? = null
        override val takeUsd: BigDecimal? = null
    }

    data class SellOrder(
        override val makePriceUsd: BigDecimal,
        override val takeUsd: BigDecimal
    ) : OrderUsdValue() {
        override val makeUsd: BigDecimal? = null
        override val takePriceUsd: BigDecimal? = null
    }
}
