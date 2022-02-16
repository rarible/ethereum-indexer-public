package com.rarible.protocol.order.core.model

import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Bytes
import io.daonomic.rpc.domain.Word
import scalether.domain.Address
import java.math.BigInteger

sealed class Transfer {
    data class Erc721Transfer(
        val from: Address,
        val to: Address,
        val tokenId: BigInteger,
        val safe: Boolean,
    ) : Transfer()

    data class Erc1155Transfer(
        val from: Address,
        val to: Address,
        val tokenId: BigInteger,
        val value: BigInteger,
        val data: Binary = Binary.empty()
    ) : Transfer()

    data class MerkleValidatorErc721Trandfer(
        val from: Address,
        val to: Address,
        val token: Address,
        val tokenId: BigInteger,
        val root: Word,
        val proof: List<Word>,
        val safe: Boolean
    ) : Transfer()

    data class MerkleValidatorErc1155Trandfer(
        val from: Address,
        val to: Address,
        val token: Address,
        val tokenId: BigInteger,
        val amount: BigInteger,
        val root: Word,
        val proof: List<Word>,
    ) : Transfer()
}
