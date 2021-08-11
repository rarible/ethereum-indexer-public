package com.rarible.protocol.order.core.model

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.Tuples
import com.rarible.protocol.contracts.Tuples.keccak256
import io.daonomic.rpc.domain.Word
import scala.Tuple2
import scala.Tuple3

data class Asset(
    val type: AssetType,
    val value: EthUInt256
) {
    fun forPeople() = Tuple2(type.forPeople(), value.value)

    fun forTx() = Tuple2(type.forTx(), value.value)

    companion object {
        fun hash(asset: Asset): Word = keccak256(Tuples.assetHashType().encode(Tuple3.apply(
            TYPE_HASH.bytes(),
            AssetType.hash(asset.type).bytes(),
            asset.value.value
        )))

        private val TYPE_HASH: Word = keccak256("Asset(AssetType assetType,uint256 value)AssetType(bytes4 assetClass,bytes data)")
    }
}
