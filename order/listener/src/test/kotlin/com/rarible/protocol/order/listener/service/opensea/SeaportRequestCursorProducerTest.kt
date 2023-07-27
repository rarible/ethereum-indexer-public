package com.rarible.protocol.order.listener.service.opensea

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class SeaportRequestCursorProducerTest {
    @Test
    fun `should produce next cursors`() {
        val cursor = "cj0xJi1waz02Mjc3NzQ4OTQz" // r=1&-pk=6277748943
        val cursors = SeaportRequestCursorProducer.produceNextFromCursor(cursor = cursor, step = 50, amount = 5)
        assertThat(cursors).hasSize(5)
        assertThat(cursors[0]).isEqualTo("cj0xJi1waz02Mjc3NzQ4OTkz") // r=1&-pk=6277748993
        assertThat(cursors[1]).isEqualTo("cj0xJi1waz02Mjc3NzQ5MDQz") // r=1&-pk=6277749043
        assertThat(cursors[2]).isEqualTo("cj0xJi1waz02Mjc3NzQ5MDkz") // r=1&-pk=6277749093
        assertThat(cursors[3]).isEqualTo("cj0xJi1waz02Mjc3NzQ5MTQz") // r=1&-pk=6277749143
    }

    @Test
    fun `should produce empty list if unrecognized cursor`() {
        listOf("LXBrPTYyNzk4NDIzMjg=", "test").forEach { cursor ->
            val cursors = SeaportRequestCursorProducer.produceNextFromCursor(cursor = cursor, step = 50, amount = 5)
            assertThat(cursors).hasSize(0)
        }
    }
}
