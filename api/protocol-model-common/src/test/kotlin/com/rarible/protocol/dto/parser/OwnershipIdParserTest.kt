package com.rarible.protocol.dto.parser

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import scalether.domain.Address

class OwnershipIdParserTest {

    @Test
    fun parse() {
        val value = "0x0000000000001b84b1cb32787b0d64758d019317:123:0xa7ee407497b2aeb43580cabe2b04026b5419d1dc"
        val ownershipId = OwnershipIdParser.parse(value)
        assertThat(ownershipId.token).isEqualTo(Address.apply("0x0000000000001b84b1cb32787b0d64758d019317"))
        assertThat(ownershipId.tokenId).isEqualTo("123")
        assertThat(ownershipId.owner).isEqualTo(Address.apply("0xa7ee407497b2aeb43580cabe2b04026b5419d1dc"))
    }
}
