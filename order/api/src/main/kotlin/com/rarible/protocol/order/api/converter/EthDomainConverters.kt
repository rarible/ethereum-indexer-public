package com.rarible.protocol.order.api.converter

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.exception.ValidationApiException
import scalether.domain.Address

fun String.toAddress(): Address = try {
    Address.apply(this)
} catch (e: Exception) {
    throw ValidationApiException("Address $this format is invalid: ${e.message}")
}

fun String.toEthUInt256(): EthUInt256 = try {
    EthUInt256.of(this)
} catch (e: Exception) {
    throw ValidationApiException("Number $this format is invalid: ${e.message}")
}