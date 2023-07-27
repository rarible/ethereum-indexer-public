package com.rarible.protocol.order.core.model

import scalether.domain.Address
import java.math.BigInteger

data class PoolInfo(
    val collection: Address,
    val curve: Address,
    val spotPrice: BigInteger,
    val delta: BigInteger,
    val fee: BigInteger,
    val protocolFee: BigInteger,
    val token: Address
) {
    val currencyAssetType
        get() = when (token) {
            Address.ZERO() -> EthAssetType
            else -> Erc20AssetType(token)
        }
}
