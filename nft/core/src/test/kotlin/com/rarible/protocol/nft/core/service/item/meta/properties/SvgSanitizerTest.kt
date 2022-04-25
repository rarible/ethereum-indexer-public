package com.rarible.protocol.nft.core.service.item.meta.properties

import com.rarible.protocol.nft.core.data.createRandomItemId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SvgSanitizerTest {

    @Test
    fun `not a svg`() {
        val url = "http://localhost:8080/<svg>"

        val result = SvgSanitizer.sanitize(createRandomItemId(), url)

        assertThat(result).isEqualTo(url)
    }

    @Test
    fun `not a svg - url with query param`() {
        // Should not be discovered as SVG
        val url = "http://localhost:8080/%3Csvg%3E"

        val result = SvgSanitizer.sanitize(createRandomItemId(), url)

        assertThat(result).isEqualTo(url)
    }

    @Test
    fun `regular svg`() {
        val svg = " <svg></svg> \n\t\r"

        val result = SvgSanitizer.sanitize(createRandomItemId(), svg)

        assertThat(result).isEqualTo("<svg></svg>")
    }

    @Test
    fun `encoded svg`() {
        val svg = "%20%3Csvg%3E%3C%2Fsvg%3E"

        val result = SvgSanitizer.sanitize(createRandomItemId(), svg)

        assertThat(result).isEqualTo("<svg></svg>")
    }

    @Test
    fun `base64 svg`() {
        val svg = "data:image/svg+xml;base64, IDxzdmc+PC9zdmc+IA== " // with spaces

        val result = SvgSanitizer.sanitize(createRandomItemId(), svg)

        assertThat(result).isEqualTo("<svg></svg>")
    }

    @Test
    fun `base64 encoded svg`() {
        val svg = "data:image/svg+xml;base64, JTIwJTNDc3ZnJTNFJTNDJTJGc3ZnJTNF " // with spaces

        val result = SvgSanitizer.sanitize(createRandomItemId(), svg)

        assertThat(result).isEqualTo("<svg></svg>")
    }

    @Test
    fun `svg with data type`() {
        val svg = "data:image/svg+xml;utf8,\n<svg></svg> "

        val result = SvgSanitizer.sanitize(createRandomItemId(), svg)

        assertThat(result).isEqualTo("<svg></svg>")
    }

    @Test
    fun `encoded svg with data type`() {
        val svg = "data:image/svg+xml;utf8,%20%3Csvg%3E%3C%2Fsvg%3E"

        val result = SvgSanitizer.sanitize(createRandomItemId(), svg)

        assertThat(result).isEqualTo("<svg></svg>")
    }
}