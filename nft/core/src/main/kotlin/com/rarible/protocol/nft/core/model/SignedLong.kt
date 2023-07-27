package com.rarible.protocol.nft.core.model

import io.daonomic.rpc.domain.Binary
import java.math.BigInteger

data class SignedLong(
    val value: Long,
    val v: Byte,
    val r: Binary,
    val s: Binary
)

fun Byte.toEth(): BigInteger = BigInteger.valueOf(this.toLong())
