package com.rarible.protocol.order.core.model

import com.rarible.protocol.order.core.data.createOrderBasicSeaportDataV1
import com.rarible.protocol.order.core.data.createOrderDataLegacy
import com.rarible.protocol.order.core.data.createOrderLooksrareDataV1
import com.rarible.protocol.order.core.data.createOrderLooksrareDataV2
import com.rarible.protocol.order.core.data.createOrderOpenSeaV1DataV1
import com.rarible.protocol.order.core.data.createOrderRaribleV1DataV3Buy
import com.rarible.protocol.order.core.data.createOrderRaribleV1DataV3Sell
import com.rarible.protocol.order.core.data.createOrderRaribleV2DataV1
import com.rarible.protocol.order.core.data.createOrderRaribleV2DataV2
import com.rarible.protocol.order.core.data.createOrderSudoSwapAmmDataV1
import com.rarible.protocol.order.core.data.createOrderX2Y2DataV1
import com.rarible.protocol.order.core.misc.MAPPER
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

internal class OrderDataTest {
    private companion object {
        private val orderData = listOf<Pair<OrderData, Class<*>>>(
            createOrderDataLegacy() to OrderDataLegacy::class.java,
            createOrderRaribleV2DataV1() to OrderRaribleV2DataV1::class.java,
            createOrderRaribleV2DataV2() to OrderRaribleV2DataV2::class.java,
            createOrderRaribleV1DataV3Sell() to OrderRaribleV2DataV3Sell::class.java,
            createOrderRaribleV1DataV3Buy() to OrderRaribleV2DataV3Buy::class.java,
            createOrderOpenSeaV1DataV1() to OrderOpenSeaV1DataV1::class.java,
            createOrderBasicSeaportDataV1() to OrderBasicSeaportDataV1::class.java,
            createOrderX2Y2DataV1() to OrderX2Y2DataV1::class.java,
            OrderCryptoPunksData to OrderCryptoPunksData::class.java,
            createOrderLooksrareDataV1() to OrderLooksrareDataV1::class.java,
            createOrderLooksrareDataV2() to OrderLooksrareDataV2::class.java,
            createOrderSudoSwapAmmDataV1() to OrderSudoSwapAmmDataV1::class.java,
        )

        @JvmStatic
        fun orderDataStream(): Stream<Arguments> = run {
            require(
                orderData
                    .map { it.first.version }
                    .containsAll(OrderDataVersion.values().toList())
            )
            orderData.stream().map { Arguments.of(it.first, it.second) }
        }
    }

    @ParameterizedTest
    @MethodSource("orderDataStream")
    fun `serialize and deserialize - ok`(orderData: OrderData, orderDataClass: Class<*>) {
        val json = MAPPER.writeValueAsString(orderData)
        val deserialized = MAPPER.readValue(json, orderDataClass)
        Assertions.assertThat(deserialized).isEqualTo(orderData)
    }

    @Test
    fun `looksrare V2 order data is make filled`() {
        val orderData: OrderData = createOrderLooksrareDataV2()
        assertThat(orderData.isMakeFillOrder(sell = true)).isEqualTo(true)
    }
}
