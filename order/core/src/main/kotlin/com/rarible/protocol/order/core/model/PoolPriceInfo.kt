package com.rarible.protocol.order.core.model

import java.math.BigInteger

sealed class PoolPriceInfo {
    abstract val nftNumber: Int
    abstract val amount: BigInteger
}

data class PoolBuyInfo(
    override val nftNumber: Int,
    override val amount: BigInteger
) : PoolPriceInfo() {
    companion object {
        val ZERO = PoolBuyInfo(0, BigInteger.ZERO)
    }
}

data class PoolSellInfo(
    override val nftNumber: Int,
    override val amount: BigInteger
) : PoolPriceInfo() {
    companion object {
        val ZERO = PoolSellInfo(0, BigInteger.ZERO)
    }
}

fun PoolPriceInfo.isZero(): Boolean = nftNumber == 0 && amount == BigInteger.ZERO
fun PoolPriceInfo.orNull(): PoolPriceInfo? = if (this.isZero()) null else this
