package com.rarible.protocol.order.core.converters.dto

import com.rarible.protocol.dto.SeaportOrderTypeDto
import com.rarible.protocol.order.core.model.SeaportOrderType
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

internal class SeaportOrderTypeDtoConverterTest {
    @Test
    fun `should convert type`() {
        Assertions.assertThat(SeaportOrderTypeDtoConverter.convert(SeaportOrderType.FULL_OPEN))
            .isEqualTo(SeaportOrderTypeDto.FULL_OPEN)
        Assertions.assertThat(SeaportOrderTypeDtoConverter.convert(SeaportOrderType.FULL_RESTRICTED))
            .isEqualTo(SeaportOrderTypeDto.FULL_RESTRICTED)
        Assertions.assertThat(SeaportOrderTypeDtoConverter.convert(SeaportOrderType.PARTIAL_OPEN))
            .isEqualTo(SeaportOrderTypeDto.PARTIAL_OPEN)
        Assertions.assertThat(SeaportOrderTypeDtoConverter.convert(SeaportOrderType.PARTIAL_RESTRICTED))
            .isEqualTo(SeaportOrderTypeDto.PARTIAL_RESTRICTED)

        Assertions.assertThat(SeaportOrderType.values()).hasSize(4)
        Assertions.assertThat(SeaportOrderTypeDto.values()).hasSize(4)
    }
}