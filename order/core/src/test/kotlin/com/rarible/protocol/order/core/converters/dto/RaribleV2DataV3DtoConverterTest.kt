package com.rarible.protocol.order.core.converters.dto

import com.rarible.protocol.dto.PartDto
import com.rarible.protocol.order.core.data.createOrderRaribleV1DataV3Buy
import com.rarible.protocol.order.core.data.createOrderRaribleV1DataV3Sell
import com.rarible.protocol.order.core.model.Part
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class RaribleV2DataV3DtoConverterTest {

    @Test
    fun `should convert order data v2 sell tp dto`() {
        val sellDataV3 = createOrderRaribleV1DataV3Sell()
        val dto = RaribleV2DataV3DtoConverter.convert(sellDataV3)

        comparePart(dto.payout!!, sellDataV3.payout!!)
        comparePart(dto.originFeeFirst!!, sellDataV3.originFeeFirst!!)
        comparePart(dto.originFeeSecond!!, sellDataV3.originFeeSecond!!)
        assertThat(dto.marketplaceMarker!!).isEqualTo(sellDataV3.marketplaceMarker)
        assertThat(dto.maxFeesBasePoint).isEqualTo(sellDataV3.maxFeesBasePoint.value.intValueExact())
    }

    @Test
    fun `should convert order data v2 buy to dto`() {
        val sellDataV3 = createOrderRaribleV1DataV3Buy()
        val dto = RaribleV2DataV3DtoConverter.convert(sellDataV3)

        comparePart(dto.payout!!, sellDataV3.payout!!)
        comparePart(dto.originFeeFirst!!, sellDataV3.originFeeFirst!!)
        comparePart(dto.originFeeSecond!!, sellDataV3.originFeeSecond!!)
        assertThat(dto.marketplaceMarker!!).isEqualTo(sellDataV3.marketplaceMarker)
    }

    private fun comparePart(dto: PartDto, source: Part) {
        assertThat(dto.account).isEqualTo(source.account)
        assertThat(dto.value).isEqualTo(source.value.value.intValueExact())
    }
}
