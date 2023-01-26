package com.rarible.protocol.order.core.model

import io.daonomic.rpc.domain.Word
import scalether.domain.Address
import java.math.BigInteger

sealed class SeaportOrderData {
    abstract val offerer: Address
    abstract val zone: Address
    abstract val offer: List<SeaportOffer>
    abstract val consideration: List<SeaportConsideration>
    abstract val orderType: SeaportOrderType
    abstract val startTime: BigInteger
    abstract val endTime: BigInteger
    abstract val zoneHash: Word
    abstract val salt: BigInteger
    abstract val conduitKey: Word
}

data class SeaportOrderComponents(
    override val offerer: Address,
    override val zone: Address,
    override val offer: List<SeaportOffer>,
    override val consideration: List<SeaportConsideration>,
    override val orderType: SeaportOrderType,
    override val startTime: BigInteger,
    override val endTime: BigInteger,
    override val zoneHash: Word,
    override val salt: BigInteger,
    override val conduitKey: Word,
    val counter: Long
) : SeaportOrderData()

data class SeaportOrderParameters(
    override val offerer: Address,
    override val zone: Address,
    override val offer: List<SeaportOffer>,
    override val consideration: List<SeaportConsideration>,
    override val orderType: SeaportOrderType,
    override val startTime: BigInteger,
    override val endTime: BigInteger,
    override val zoneHash: Word,
    override val salt: BigInteger,
    override val conduitKey: Word,
    val totalOriginalConsiderationItems: Long
) : SeaportOrderData()
