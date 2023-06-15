package com.rarible.protocol.order.api.converter

import com.rarible.protocol.dto.OrderFormDto
import com.rarible.protocol.order.core.model.Platform
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OrderFormPlatformConverterTest {
    @Test
    fun `convert rarible`() {
        assertThat(OrderFormPlatformConverter.convert(OrderFormDto.Platform.RARIBLE)).isEqualTo(Platform.RARIBLE)
    }

    @Test
    fun `convert cmp`() {
        assertThat(OrderFormPlatformConverter.convert(OrderFormDto.Platform.CMP)).isEqualTo(Platform.CMP)
    }

    @Test
    fun `convert defult`() {
        assertThat(OrderFormPlatformConverter.convert(null)).isEqualTo(Platform.RARIBLE)
    }
}