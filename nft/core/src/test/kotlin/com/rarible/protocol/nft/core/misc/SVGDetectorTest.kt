package com.rarible.protocol.nft.core.misc

import com.rarible.protocol.nft.core.misc.detector.SVGDetector
import com.rarible.protocol.nft.core.service.IpfsService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths

class SVGDetectorTest {

    private val svgUrl = "${IpfsService.RARIBLE_IPFS}/data:image/svg+xml;utf8,<svg%20class='nft'><rect%20class='c217'%20x='10'%20y='12'%20width='2'%20height='1' fill:%23AAAAAA/></svg>"
    private val decodedSvg = "<svg class='nft'><rect class='c217' x='10' y='12' width='2' height='1' fill:#AAAAAA/></svg>"

    @Test
    fun `svg detector do not react to strings without svg tag`() {
        val sVGDetector = SVGDetector("url")
        val result = sVGDetector.canDecode()
        Assertions.assertEquals(false, result)
    }

    @Test
    fun `svg detector is able to recognize svg tag`() {
        val sVGDetector = SVGDetector(svgUrl)
        val result = sVGDetector.canDecode()
        Assertions.assertEquals(true, result)
    }

    @Test
    fun `get svg image parts`() {
        val svgDetector = SVGDetector(svgUrl)

        assertThat(svgDetector.getData()).isEqualTo(decodedSvg)
        assertThat(svgDetector.getMimeType()).isEqualTo("image/svg+xml")
    }

    @Test
    fun `can decode svg images`() {
        val svg = String(Files.readAllBytes(Paths.get(this::class.java.getResource("/svg/test.svg").toURI())));

        val sVGDetector = SVGDetector(svg)
        val result = sVGDetector.canDecode()
        assertThat(result).isTrue
        assertThat(sVGDetector.getData()).isNotEmpty
    }
}
