package com.rarible.protocol.order.core.converters.dto

import com.rarible.protocol.dto.OrderBasicSeaportDataV1Dto
import com.rarible.protocol.order.core.data.randomOrderBasicSeaportDataV1
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class SeaportDataV1DtoConverterTest {
    @Test
    fun `should convert type`() {
        val source = randomOrderBasicSeaportDataV1()
        val dto = SeaportDataV1DtoConverter.convert(source)
        assertThat(dto).isInstanceOf(OrderBasicSeaportDataV1Dto::class.java)
        dto as OrderBasicSeaportDataV1Dto

        assertThat(dto.protocol).isEqualTo(source.protocol)
        assertThat(dto.zone).isEqualTo(source.zone)
        assertThat(dto.zoneHash).isEqualTo(source.zoneHash)
        assertThat(dto.conduitKey).isEqualTo(source.conduitKey)
        assertThat(dto.counter).isEqualTo(source.getCounterValue().value.toLong())
        assertThat(dto.nonce).isEqualTo(source.getCounterValue().value)
        assertThat(dto.offer).isEqualTo(source.offer.map { SeaportOfferDtoConverter.convert(it) })
        assertThat(dto.consideration).isEqualTo(source.consideration.map { SeaportConsiderationDtoConverter.convert(it) })
    }
}
