package com.rarible.protocol.order.core.model

import scalether.domain.Address
import java.math.BigInteger

data class SudoSwapNftDepositDetail(
    val pollAddress: Address,
    val collection: Address,
    val tokenIds: List<BigInteger>,
)
