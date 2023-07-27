package com.rarible.protocol.order.listener.service.sudoswap

import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import scalether.domain.Address

internal class SudoSwapEventConverterTest {
    private val converter = SudoSwapEventConverter(mockk())

    @Test
    fun `should convert poolAddress to hash`() {
        val poolAddress = Address.apply("0x6c8ba1dafb22eae61e9cd3da724cbc3d164c27b9")
        val hash = converter.getPoolHash(poolAddress)
        assertThat(hash.prefixed()).isEqualTo("0x0000000000000000000000006c8ba1dafb22eae61e9cd3da724cbc3d164c27b9")
    }
}
