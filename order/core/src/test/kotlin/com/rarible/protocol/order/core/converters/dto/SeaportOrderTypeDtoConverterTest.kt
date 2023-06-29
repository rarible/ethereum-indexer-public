package com.rarible.protocol.order.core.converters.dto

import com.rarible.protocol.dto.SeaportOrderTypeDto
import com.rarible.protocol.order.core.model.SeaportOrderType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class SeaportOrderTypeDtoConverterTest {
    @Test
    fun `should convert type`() {
        assertThat(SeaportOrderTypeDtoConverter.convert(SeaportOrderType.FULL_OPEN))
            .isEqualTo(SeaportOrderTypeDto.FULL_OPEN)
        assertThat(SeaportOrderTypeDtoConverter.convert(SeaportOrderType.FULL_RESTRICTED))
            .isEqualTo(SeaportOrderTypeDto.FULL_RESTRICTED)
        assertThat(SeaportOrderTypeDtoConverter.convert(SeaportOrderType.PARTIAL_OPEN))
            .isEqualTo(SeaportOrderTypeDto.PARTIAL_OPEN)
        assertThat(SeaportOrderTypeDtoConverter.convert(SeaportOrderType.PARTIAL_RESTRICTED))
            .isEqualTo(SeaportOrderTypeDto.PARTIAL_RESTRICTED)

        assertThat(SeaportOrderType.values()).hasSize(5)
        assertThat(SeaportOrderTypeDto.values()).hasSize(4)
    }
}