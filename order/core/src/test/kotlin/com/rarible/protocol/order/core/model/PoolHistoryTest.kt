package com.rarible.protocol.order.core.model

import com.rarible.protocol.order.core.data.randomPoolDeltaUpdate
import com.rarible.protocol.order.core.data.randomPoolFeeUpdate
import com.rarible.protocol.order.core.data.randomPoolNftDeposit
import com.rarible.protocol.order.core.data.randomPoolNftWithdraw
import com.rarible.protocol.order.core.data.randomPoolSpotPriceUpdate
import com.rarible.protocol.order.core.data.randomPoolTargetNftIn
import com.rarible.protocol.order.core.data.randomPoolTargetNftOut
import com.rarible.protocol.order.core.data.randomSellOnChainAmmOrder
import com.rarible.protocol.order.core.misc.MAPPER
import org.assertj.core.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

internal class PoolHistoryTest {
    private companion object {
        private val poolHistories = listOf<Pair<PoolHistory, Class<*>>>(
            randomSellOnChainAmmOrder() to PoolCreate::class.java,
            randomPoolTargetNftOut() to PoolTargetNftOut::class.java,
            randomPoolTargetNftIn() to PoolTargetNftIn::class.java,
            randomPoolNftWithdraw() to PoolNftWithdraw::class.java,
            randomPoolNftDeposit() to PoolNftDeposit::class.java,
            randomPoolSpotPriceUpdate() to PoolSpotPriceUpdate::class.java,
            randomPoolDeltaUpdate() to PoolDeltaUpdate::class.java,
            randomPoolFeeUpdate() to PoolFeeUpdate::class.java,
        )

        @JvmStatic
        fun assetTypesStream(): Stream<Arguments> = run {
            require(
                poolHistories
                    .map { it.first.type }
                    .containsAll(PoolHistoryType.values().toList())
            )
            poolHistories.stream().map { Arguments.of(it.first, it.second) }
        }
    }

    @ParameterizedTest
    @MethodSource("assetTypesStream")
    fun `serialize and deserialize - ok`(poolHistory: PoolHistory, assetTypeClass: Class<*>) {
        val jsonAssetType = MAPPER.writeValueAsString(poolHistory)
        val deserializedAssetType = MAPPER.readValue(jsonAssetType, assetTypeClass)
        Assertions.assertThat(deserializedAssetType).isEqualTo(poolHistory)
    }
}
