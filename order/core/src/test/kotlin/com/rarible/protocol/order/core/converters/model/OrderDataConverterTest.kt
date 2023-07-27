package com.rarible.protocol.order.core.converters.model

import com.rarible.protocol.dto.PartDto
import com.rarible.protocol.order.core.data.createOrderRaribleV1DataV3BuyDto
import com.rarible.protocol.order.core.data.createOrderRaribleV1DataV3SellDto
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV3Buy
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV3Sell
import com.rarible.protocol.order.core.model.Part
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

internal class OrderDataConverterTest {
    @Test
    fun `should convert order data v2 sell`() {
        val dto = createOrderRaribleV1DataV3SellDto()
        val model = OrderDataConverter.convert(dto) as OrderRaribleV2DataV3Sell

        comparePart(model.payout!!, dto.payout!!)
        comparePart(model.originFeeFirst!!, dto.originFeeFirst!!)
        comparePart(model.originFeeSecond!!, dto.originFeeSecond!!)
        Assertions.assertThat(model.marketplaceMarker!!).isEqualTo(dto.marketplaceMarker)
        Assertions.assertThat(model.maxFeesBasePoint.value.intValueExact()).isEqualTo(dto.maxFeesBasePoint)
    }

    @Test
    fun `should convert order data v2 buy`() {
        val dto = createOrderRaribleV1DataV3BuyDto()
        val model = OrderDataConverter.convert(dto) as OrderRaribleV2DataV3Buy

        comparePart(model.payout!!, dto.payout!!)
        comparePart(model.originFeeFirst!!, dto.originFeeFirst!!)
        comparePart(model.originFeeSecond!!, dto.originFeeSecond!!)
        Assertions.assertThat(model.marketplaceMarker!!).isEqualTo(dto.marketplaceMarker)
    }

    private fun comparePart(model: Part, dto: PartDto) {
        Assertions.assertThat(dto.account).isEqualTo(model.account)
        Assertions.assertThat(dto.value).isEqualTo(model.value.value.intValueExact())
    }
}
