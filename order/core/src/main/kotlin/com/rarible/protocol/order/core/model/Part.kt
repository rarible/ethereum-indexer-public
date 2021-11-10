package com.rarible.protocol.order.core.model

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.Tuples
import io.daonomic.rpc.domain.Word
import scala.Tuple2
import scala.Tuple3
import scalether.domain.Address

data class Part(
    val account: Address,
    val value: EthUInt256
) {
    fun toEthereum() = Tuple2(account, value.value)

    fun hash() = hash(this)

    companion object {
        private val TYPE_HASH: Word = Tuples.keccak256("Part(address account,uint256 value)")

        fun hash(part: Part): Word = Tuples.keccak256(
            Tuples.partHashType().encode(
                Tuple3(
                    TYPE_HASH.bytes(),
                    part.account,
                    part.value.value
                )
            )
        )
    }
}
