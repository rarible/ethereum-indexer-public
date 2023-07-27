package com.rarible.protocol.order.core.converters.dto

import com.rarible.protocol.dto.SeaportItemTypeDto
import com.rarible.protocol.order.core.model.SeaportItemType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class SeaportItemTypeDtoConverterTest {
    @Test
    fun `should convert type`() {
        assertThat(SeaportItemTypeDtoConverter.convert(SeaportItemType.NATIVE))
            .isEqualTo(SeaportItemTypeDto.NATIVE)
        assertThat(SeaportItemTypeDtoConverter.convert(SeaportItemType.ERC20))
            .isEqualTo(SeaportItemTypeDto.ERC20)
        assertThat(SeaportItemTypeDtoConverter.convert(SeaportItemType.ERC721))
            .isEqualTo(SeaportItemTypeDto.ERC721)
        assertThat(SeaportItemTypeDtoConverter.convert(SeaportItemType.ERC721_WITH_CRITERIA))
            .isEqualTo(SeaportItemTypeDto.ERC721_WITH_CRITERIA)
        assertThat(SeaportItemTypeDtoConverter.convert(SeaportItemType.ERC1155_WITH_CRITERIA))
            .isEqualTo(SeaportItemTypeDto.ERC1155_WITH_CRITERIA)

        assertThat(SeaportItemType.values()).hasSize(6)
        assertThat(SeaportItemTypeDto.values()).hasSize(6)
    }
}
