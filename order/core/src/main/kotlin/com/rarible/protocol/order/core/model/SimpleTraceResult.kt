package com.rarible.protocol.order.core.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.rarible.ethereum.domain.EthUInt256
import scalether.domain.Address
import java.math.BigInteger

data class SimpleTraceResult(
    val type: String?,
    val from: Address,
    val to: Address,
    val input: String,
    val output: String,
    @JsonProperty("value")
    val valueHexString: String
) {
    /**
     * Value sent along with the transaction from "msg.sender".
     * This is specified as a HEX string, like "0x123", which is not padded on the left side with 0s
     * (and thus can't be parsed by EthUnit256.of(...) directly, which expects the string to be of even length)
     */
    @get:JsonIgnore
    val value: EthUInt256
        get() = EthUInt256.of(BigInteger(valueHexString.removePrefix("0x"), 16))
}
