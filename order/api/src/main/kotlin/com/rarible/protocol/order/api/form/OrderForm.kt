package com.rarible.protocol.order.api.form

import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.OrderData
import com.rarible.protocol.order.core.model.OrderType
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import scalether.domain.Address
import java.math.BigInteger

data class OrderForm(
    val end: Long,
    val make: Asset,
    val maker: Address,
    val salt: BigInteger,
    val signature: Binary,
    val start: Long?,
    val take: Asset,
    val taker: Address?,
    val data: OrderData,
    val type: OrderType,

    // Calculated fields
    val hash: Word,
)
