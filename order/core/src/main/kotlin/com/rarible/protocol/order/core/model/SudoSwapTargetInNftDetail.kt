package com.rarible.protocol.order.core.model

import scalether.domain.Address
import java.math.BigInteger

data class SudoSwapTargetInNftDetail(
    val minExpectedTokenOutput: BigInteger,
    val tokenRecipient: Address,
    val tokenIds: List<BigInteger>
)
