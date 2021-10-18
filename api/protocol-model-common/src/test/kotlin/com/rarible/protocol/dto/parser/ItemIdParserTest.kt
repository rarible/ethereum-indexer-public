package com.rarible.protocol.dto.parser

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import scalether.domain.Address

class ItemIdParserTest {

    @Test
    fun parse() {
        val value = "0xa7ee407497b2aeb43580cabe2b04026b5419d1dc:123"
        val itemId = ItemIdParser.parse(value)
        assertThat(itemId.token).isEqualTo(Address.apply("0xa7ee407497b2aeb43580cabe2b04026b5419d1dc"))
        assertThat(itemId.tokenId).isEqualTo("123")
    }
}
