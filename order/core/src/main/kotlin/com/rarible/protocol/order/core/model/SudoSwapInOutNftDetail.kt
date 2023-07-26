package com.rarible.protocol.order.core.model

import com.rarible.ethereum.domain.EthUInt256
import scalether.domain.Address
import java.math.BigInteger

sealed class SudoSwapOutNftDetail {
    abstract val maxExpectedTokenInput: BigInteger
    abstract val nftRecipient: Address
    abstract val outputValue: EthUInt256
}

data class SudoSwapAnyOutNftDetail(
    override val maxExpectedTokenInput: BigInteger,
    override val nftRecipient: Address,
    override val outputValue: EthUInt256,
    val numberNft: BigInteger,
) : SudoSwapOutNftDetail()

data class SudoSwapTargetOutNftDetail(
    override val maxExpectedTokenInput: BigInteger,
    override val nftRecipient: Address,
    override val outputValue: EthUInt256,
    val nft: List<BigInteger>
) : SudoSwapOutNftDetail()
