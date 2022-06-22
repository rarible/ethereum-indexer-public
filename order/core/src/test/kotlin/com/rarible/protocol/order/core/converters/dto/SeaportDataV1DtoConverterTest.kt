package com.rarible.protocol.order.core.converters.dto

import com.rarible.protocol.dto.BasicOrderSeaportDataV1Dto
import com.rarible.protocol.order.core.data.randomOrderBasicSeaportDataV1
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class SeaportDataV1DtoConverterTest {
    @Test
    fun `should convert type`() {
        val source = randomOrderBasicSeaportDataV1()
        val dto = SeaportDataV1DtoConverter.convert(source)
        assertThat(dto).isInstanceOf(BasicOrderSeaportDataV1Dto::class.java)
        dto as BasicOrderSeaportDataV1Dto

        assertThat(dto.protocol).isEqualTo(source.protocol)
        assertThat(dto.zone).isEqualTo(source.zone)
        assertThat(dto.zoneHash).isEqualTo(source.zoneHash)
        assertThat(dto.conduitKey).isEqualTo(source.conduitKey)
        assertThat(dto.counter).isEqualTo(source.counter)
        assertThat(dto.offer).isEqualTo(source.offer.map { SeaportOfferDtoConverter.convert(it) })
        assertThat(dto.consideration).isEqualTo(source.consideration.map { SeaportConsiderationDtoConverter.convert(it) })
    }
}