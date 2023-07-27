package com.rarible.protocol.order.core.converters.dto

import com.rarible.protocol.dto.SeaportItemTypeDto
import com.rarible.protocol.order.core.data.randomSeaportConsideration
import com.rarible.protocol.order.core.model.SeaportItemType
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

internal class SeaportConsiderationDtoConverterTest {
    @Test
    fun `should convert`() {
        val source = randomSeaportConsideration().copy(itemType = SeaportItemType.NATIVE)
        val dto = SeaportConsiderationDtoConverter.convert(source)
        Assertions.assertThat(dto.itemType).isEqualTo(SeaportItemTypeDto.NATIVE)
        Assertions.assertThat(dto.token).isEqualTo(source.token)
        Assertions.assertThat(dto.identifierOrCriteria).isEqualTo(source.identifier)
        Assertions.assertThat(dto.startAmount).isEqualTo(source.startAmount)
        Assertions.assertThat(dto.endAmount).isEqualTo(source.endAmount)
        Assertions.assertThat(dto.recipient).isEqualTo(source.recipient)
    }
}
