package com.rarible.protocol.order.core.model

import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import scalether.domain.Address
import java.math.BigInteger

sealed class Transfer {
    abstract val from: Address
    abstract val to: Address
    abstract val tokenId: BigInteger
    abstract val value: BigInteger

    data class Erc721Transfer(
        override val from: Address,
        override val to: Address,
        override val tokenId: BigInteger,
        val safe: Boolean,
    ) : Transfer() {
        override val value: BigInteger = BigInteger.ONE
    }

    data class Erc1155Transfer(
        override val from: Address,
        override val to: Address,
        override val tokenId: BigInteger,
        override val value: BigInteger,
        val data: Binary = Binary.empty()
    ) : Transfer()

    data class MerkleValidatorErc721Transfer(
        override val from: Address,
        override val to: Address,
        override val tokenId: BigInteger,
        val token: Address,
        val root: Word,
        val proof: List<Word>,
        val safe: Boolean
    ) : Transfer() {
        override val value: BigInteger = BigInteger.ONE
    }

    data class MerkleValidatorErc1155Transfer(
        override val from: Address,
        override val to: Address,
        override val tokenId: BigInteger,
        override val value: BigInteger,
        val token: Address,
        val root: Word,
        val proof: List<Word>,
    ) : Transfer()
}
