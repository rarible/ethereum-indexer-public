package com.rarible.protocol.dto.parser

import com.rarible.protocol.dto.ArgumentFormatException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class IdParserTest {

    @Test
    fun `split - ok`() {
        val id = "abs:123"
        val parts = IdParser.split(id, 2)

        assertThat(parts).hasSize(2)
        assertThat(parts[0]).isEqualTo("abs")
        assertThat(parts[1]).isEqualTo("123")
    }

    @Test
    fun `split - wrong size`() {
        val id = "abs:123"
        assertThrows(ArgumentFormatException::class.java) {
            IdParser.split(id, 3)
        }
    }
}
