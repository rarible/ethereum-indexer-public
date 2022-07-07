package com.rarible.protocol.order.core.model

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.Tuples
import io.daonomic.rpc.domain.Word
import scala.Tuple2
import scala.Tuple3
import scalether.abi.AddressType
import scalether.abi.Uint256Type
import scalether.abi.Uint96Type
import scalether.domain.Address
import java.math.BigInteger

data class Part(
    val account: Address,
    val value: EthUInt256
) {
    fun toEthereum() = Tuple2(account, value.value)

    fun toBigInteger(): BigInteger {
        return Uint96Type.encode(value.value).add(account).toBigInteger()
    }

    fun hash() = hash(this)

    companion object {
        private val TYPE_HASH: Word = Tuples.keccak256("Part(address account,uint256 value)")

        fun from(value: BigInteger): Part {
            val binary = Uint256Type.encode(value)
            return Part(AddressType.decode(binary, 0).value(), EthUInt256.of(Uint96Type.decode(binary, -20).value()))
        }

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
