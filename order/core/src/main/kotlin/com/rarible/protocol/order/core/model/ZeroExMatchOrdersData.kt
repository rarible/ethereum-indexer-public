package com.rarible.protocol.order.core.model

import io.daonomic.rpc.domain.Binary
import scalether.domain.Address

data class ZeroExMatchOrdersData(
    val leftOrder: ZeroExOrder,
    val rightOrder: ZeroExOrder,
    val leftSignature: Binary,
    val rightSignature: Binary,
    val feeData: List<ZeroExFeeData>,
    val paymentTokenAddress: Address,
)