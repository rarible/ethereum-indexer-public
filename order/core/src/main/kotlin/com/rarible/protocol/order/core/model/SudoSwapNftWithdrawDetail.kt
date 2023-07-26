package com.rarible.protocol.order.core.model

import scalether.domain.Address
import java.math.BigInteger

data class SudoSwapNftWithdrawDetail(
    val collection: Address,
    val nft: List<BigInteger>
)
