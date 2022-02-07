package com.rarible.protocol.nft.core.misc

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class Base64DetectorTest {

    private val base64 = "https://rarible.mypinata.cloud/data:image/png;base64,abc"

    @Test
    fun `is base64 url`() {
        val base64 = Base64Detector(base64)
        val regularUrl = Base64Detector("https://rarible.mypinata.cloud/ipfs/abc/image.png")

        assertThat(base64.isBase64Image).isTrue()
        assertThat(regularUrl.isBase64Image).isFalse()
    }

    @Test
    fun `get base64 image parts`() {
        val base64 = Base64Detector(base64)

        assertThat(base64.getBase64Data()).isEqualTo("abc")
        assertThat(base64.getBase64MimeType()).isEqualTo("image/png")
    }

}