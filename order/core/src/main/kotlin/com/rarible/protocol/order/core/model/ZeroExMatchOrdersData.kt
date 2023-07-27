package com.rarible.protocol.order.core.model

import io.daonomic.rpc.domain.Binary
import scalether.domain.Address

data class ZeroExMatchOrdersData(
    val leftOrder: ZeroExOrder,
    val takerAddress: Address?,
    val rightOrder: ZeroExOrder? = null,
    val leftSignature: Binary,
    val rightSignature: Binary? = null,
    val feeData: List<ZeroExFeeData> = listOf(),
    val paymentTokenAddress: Address = Address.ZERO(),
)
