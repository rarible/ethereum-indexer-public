package com.rarible.protocol.order.core.model

import scalether.domain.Address
import java.math.BigInteger

sealed class SudoSwapPairDetail {
    abstract val nft: Address
    abstract val bondingCurve: Address
    abstract val assetRecipient: Address
    abstract val poolType: SudoSwapPoolType
    abstract val delta: BigInteger
    abstract val fee: BigInteger
    abstract val inNft: List<BigInteger>
    abstract val spotPrice: BigInteger
}

class SudoSwapEthPairDetail(
    override val nft: Address,
    override val bondingCurve: Address,
    override val assetRecipient: Address,
    override val poolType: SudoSwapPoolType,
    override val delta: BigInteger,
    override val fee: BigInteger,
    override val spotPrice: BigInteger,
    override val inNft: List<BigInteger>,
    val ethBalance: BigInteger,
) : SudoSwapPairDetail()

class SudoSwapErc20PairDetail(
    override val nft: Address,
    override val bondingCurve: Address,
    override val assetRecipient: Address,
    override val poolType: SudoSwapPoolType,
    override val delta: BigInteger,
    override val fee: BigInteger,
    override val spotPrice: BigInteger,
    override val inNft: List<BigInteger>,
    val token: Address,
    val tokenBalance: BigInteger,
) : SudoSwapPairDetail()
