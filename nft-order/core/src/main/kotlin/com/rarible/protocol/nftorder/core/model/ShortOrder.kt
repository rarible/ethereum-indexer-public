package com.rarible.protocol.nftorder.core.model

import io.daonomic.rpc.domain.Word
import java.math.BigDecimal
import java.math.BigInteger

data class ShortOrder(
    val hash: Word,
    val platform: String,
    val makeStock: BigInteger,
    val makePriceUsd: BigDecimal?,
    val takePriceUsd: BigDecimal?
)