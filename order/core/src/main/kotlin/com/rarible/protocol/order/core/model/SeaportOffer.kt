package com.rarible.protocol.order.core.model

import scalether.domain.Address
import java.math.BigInteger

data class SeaportOffer(
    val itemType: SeaportItemType,
    val token: Address,
    val identifierOrCriteria: BigInteger,
    val startAmount: BigInteger,
    val endAmount: BigInteger
)