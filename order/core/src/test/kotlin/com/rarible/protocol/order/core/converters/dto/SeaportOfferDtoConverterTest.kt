package com.rarible.protocol.order.core.converters.dto

import com.rarible.protocol.dto.SeaportItemTypeDto
import com.rarible.protocol.order.core.data.randomSeaportOffer
import com.rarible.protocol.order.core.model.SeaportItemType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class SeaportOfferDtoConverterTest {
    @Test
    fun `should convert`() {
        val source = randomSeaportOffer().copy(itemType = SeaportItemType.ERC721)
        val dto = SeaportOfferDtoConverter.convert(source)
        assertThat(dto.itemType).isEqualTo(SeaportItemTypeDto.ERC721)
        assertThat(dto.token).isEqualTo(source.token)
        assertThat(dto.identifierOrCriteria).isEqualTo(source.identifier)
        assertThat(dto.startAmount).isEqualTo(source.startAmount)
        assertThat(dto.endAmount).isEqualTo(source.endAmount)
    }
}
