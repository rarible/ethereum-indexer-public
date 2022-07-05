package com.rarible.protocol.order.core.model

import io.daonomic.rpc.domain.Word
import scalether.domain.Address
import java.math.BigInteger

data class SeaportOrderComponents(
    val offerer: Address,
    val zone: Address,
    val offer: List<SeaportOffer>,
    val consideration: List<SeaportConsideration>,
    val orderType: SeaportOrderType,
    val startTime: Long,
    val endTime: Long,
    val zoneHash: Word,
    val salt: BigInteger,
    val conduitKey: Word,
    val counter: Long
)