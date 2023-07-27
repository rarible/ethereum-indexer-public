package com.rarible.protocol.order.core.model

import io.daonomic.rpc.domain.Binary
import scalether.domain.Address

data class PrepareTxResponse(
    val transferProxyAddress: Address?,
    val asset: Asset,
    val transaction: PreparedTx
)

data class PreparedTx(
    val to: Address,
    val data: Binary
)
