package com.rarible.protocol.order.core.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.springframework.data.annotation.Transient
import scalether.domain.Address
import java.math.BigInteger

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "version")
@JsonSubTypes(
    JsonSubTypes.Type(value = SudoSwapPoolDataV1::class, name = "SUDOSWAP_POOL_DATA_V1"),
)
sealed class PoolData {
    abstract val version: PoolDataVersion

    abstract val poolAddress: Address
    abstract fun toOrderData(): OrderAmmData
}

data class SudoSwapPoolDataV1(
    override val poolAddress: Address,
    val bondingCurve: Address,
    val curveType: SudoSwapCurveType,
    val assetRecipient: Address,
    val factory: Address,
    val poolType: SudoSwapPoolType,
    val spotPrice: BigInteger,
    val delta: BigInteger,
    val fee: BigInteger
) : PoolData() {
    @get:Transient
    override val version = PoolDataVersion.SUDOSWAP_POOL_DATA_V1

    override fun toOrderData(): OrderSudoSwapAmmDataV1 {
        return OrderSudoSwapAmmDataV1(
            poolAddress = poolAddress,
            bondingCurve = bondingCurve,
            factory = factory,
            curveType = curveType,
            assetRecipient = assetRecipient,
            poolType = poolType,
            spotPrice = spotPrice,
            delta = delta,
            fee = fee
        )
    }
}

enum class PoolDataVersion {
    SUDOSWAP_POOL_DATA_V1,
}
