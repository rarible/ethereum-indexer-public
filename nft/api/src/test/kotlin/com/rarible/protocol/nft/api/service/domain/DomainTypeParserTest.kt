package com.rarible.protocol.nft.api.service.domain

import com.rarible.protocol.dto.ArgumentFormatException
import com.rarible.protocol.nft.api.model.DomainType
import com.rarible.protocol.nft.core.data.randomEnsDomain
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DomainTypeParserTest {
    @Test
    fun `test - ok`() {
        val valid = randomEnsDomain()
        val type = DomainTypeParser.parse(valid)
        assertThat(type).isEqualTo(DomainType.ENS)
    }

    @Test
    fun `test - false, invalid domain format`() {
        assertThrows<ArgumentFormatException> {
            DomainTypeParser.parse("invalid")
        }
    }

    @Test
    fun `test - false, unsupported tld`() {
        val type = DomainTypeParser.parse("unsupported.xyz")
        assertThat(type).isNull()
    }
}
