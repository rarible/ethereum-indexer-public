package com.rarible.protocol.nftorder.core.data

import java.math.BigInteger

data class ItemSellStats(
    val sellers: Int,
    val totalStock: BigInteger
)