package com.rarible.protocol.nft.core.misc

import com.rarible.protocol.nft.core.misc.detector.Base64Detector
import com.rarible.protocol.nft.core.service.IpfsService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class Base64DetectorTest {

    private val base64 = "${IpfsService.RARIBLE_IPFS}/data:image/png;base64,abc"
    private val base64TextType = "${IpfsService.RARIBLE_IPFS}/data:text/html;base64,abc"

    @Test
    fun `is base64 url`() {
        val base64 = Base64Detector(base64)
        val regularUrl = Base64Detector("${IpfsService.RARIBLE_IPFS}/ipfs/abc/image.png")

        assertThat(base64.canDecode()).isTrue
        assertThat(regularUrl.canDecode()).isFalse
    }

    @Test
    fun `get base64 image parts`() {
        val base64 = Base64Detector(base64)

        assertThat(base64.getData()).isEqualTo("abc")
        assertThat(base64.getMimeType()).isEqualTo("image/png")
    }

    @Test
    fun `get base64 image test text type`() {
        val base64 = Base64Detector(base64TextType)

        assertThat(base64.getData()).isEqualTo("abc")
        assertThat(base64.getMimeType()).isEqualTo("text/html")
    }
}
