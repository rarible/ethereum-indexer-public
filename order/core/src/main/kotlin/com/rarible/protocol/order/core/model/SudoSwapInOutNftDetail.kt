package com.rarible.protocol.order.core.model

import scalether.domain.Address
import java.math.BigInteger

sealed class SudoSwapOutNftDetail {
    abstract val maxExpectedTokenInput: BigInteger
    abstract val nftRecipient: Address
}

data class SudoSwapAnyOutNftDetail(
    override val maxExpectedTokenInput: BigInteger,
    override val nftRecipient: Address,
    val numberNft: BigInteger
) : SudoSwapOutNftDetail()

data class SudoSwapTargetOutNftDetail(
    override val maxExpectedTokenInput: BigInteger,
    override val nftRecipient: Address,
    val nft: List<BigInteger>
) : SudoSwapOutNftDetail()