package com.rarible.protocol.order.core.parser

import com.rarible.protocol.order.core.data.createOrderRaribleV1DataV3Buy
import com.rarible.protocol.order.core.data.createOrderRaribleV1DataV3Sell
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV3Buy
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV3Sell
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ExchangeV2OrderDataParserTest {

    @Test
    fun `should parse order data v3 sell`() {
        val expected = createOrderRaribleV1DataV3Sell()

        val model = ExchangeV2OrderDataParser.parse(
            expected.version.ethDataType!!,
            expected.toEthereum()
        )

        assertThat(model).isInstanceOf(OrderRaribleV2DataV3Sell::class.java)

        with(model as OrderRaribleV2DataV3Sell) {
            assertThat(payout!!).isEqualTo(expected.payout)
            assertThat(originFeeFirst!!).isEqualTo(expected.originFeeFirst)
            assertThat(originFeeSecond!!).isEqualTo(expected.originFeeSecond)
            assertThat(maxFeesBasePoint).isEqualTo(expected.maxFeesBasePoint)
            assertThat(marketplaceMarker!!).isEqualTo(expected.marketplaceMarker)
        }
    }

    @Test
    fun `should parse order data v3 buy`() {
        val expected = createOrderRaribleV1DataV3Buy()
        val model = ExchangeV2OrderDataParser.parse(expected.version.ethDataType!!, expected.toEthereum())
        assertThat(model).isInstanceOf(OrderRaribleV2DataV3Buy::class.java)
        with(model as OrderRaribleV2DataV3Buy) {
            assertThat(payout!!).isEqualTo(expected.payout)
            assertThat(originFeeFirst!!).isEqualTo(expected.originFeeFirst)
            assertThat(originFeeSecond!!).isEqualTo(expected.originFeeSecond)
            assertThat(marketplaceMarker!!).isEqualTo(expected.marketplaceMarker)
        }
    }
}
