package com.rarible.protocol.order.core.model

import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import scalether.domain.Address
import java.math.BigInteger

data class BlurOrder(
    val trader: Address,
    val side: BlurOrderSide,
    val matchingPolicy: Address,
    val collection: Address,
    val tokenId: BigInteger,
    val amount: BigInteger,
    val paymentToken: Address,
    val price: BigInteger,
    val listingTime: BigInteger,
    val expirationTime: BigInteger,
    val fees: List<BlurFee>,
    val salt: BigInteger,
    val extraParams: Binary,
) {
    fun hash(counter: Long): Word {
        return Order.blurV1Hash(this, counter.toBigInteger())
    }
}
