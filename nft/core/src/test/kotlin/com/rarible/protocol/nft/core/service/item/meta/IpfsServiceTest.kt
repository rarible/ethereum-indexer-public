package com.rarible.protocol.nft.core.service.item.meta

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@ItemMetaTest
class IpfsServiceTest : BasePropertiesResolverTest() {

    private val service = ipfsService
    private val cid = "QmbpJhWFiwzNu7MebvKG3hrYiyWmSiz5dTUYMQLXsjT9vw"

    @Test
    fun `is cid`() {
        assertThat(service.isCid("QmQzqPpcBFkc9AwV4B2tscLy9dBwN7o9yEHE9aRCHeN6KW")).isTrue()
        assertThat(service.isCid("QQzqPpcBFkc9AwV4B2tscLy9dBwN7o9yEHE9aRCHeN6KW")).isFalse()
        assertThat(service.isCid("bafybeigdyrzt5sfp7udm7hu76uh7y26nf3efuylqabf3oclgtqy55fbzdi")).isTrue()
        assertThat(service.isCid("3")).isFalse()
        assertThat(service.isCid("f01701220c3c4733ec8affd06cf9e9ff50ffc6bcd2ec85a6170004bb709669c31de94391a")).isTrue()
    }

    @Test
    fun `svg file with CID urls`() {
        val svg = "<svg url=https://ipfs.io/ipfs/QmQzqPpcBFkc9AwV4B2tscLy9dBwN7o9yEHE9aRCHeN6KW></svg>"
        val result = service.resolvePublicHttpUrl(svg)
        // should stay as SVG
        assertThat(result).isEqualTo(svg)
    }

    @Test
    fun `foreign ipfs urls - replaced by public gateway`() {
        // Broken IPFS URL
        assertFixedIpfsUrl("htt://mypinata.com/ipfs/$cid", cid)
        // Relative IPFS path
        assertFixedIpfsUrl("/ipfs/$cid/abc .png", "$cid/abc%20.png")

        // Abstract IPFS urls with /ipfs/ path and broken slashes
        assertFixedIpfsUrl("ipfs:/ipfs/$cid", cid)
        assertFixedIpfsUrl("ipfs://ipfs/$cid", cid)
        assertFixedIpfsUrl("ipfs:///ipfs/$cid", cid)
        assertFixedIpfsUrl("ipfs:////ipfs/$cid", cid)

        assertFixedIpfsUrl("ipfs:////ipfs/$cid", cid)
        assertFixedIpfsUrl("ipfs:////ipfs//$cid", cid)
        assertFixedIpfsUrl("ipfs:////ipfs///$cid", cid)
    }

    @Test
    fun `foreign ipfs urls - original gateway kept`() {
        // Regular IPFS URL
        assertOriginalIpfsUrl("https://ipfs.io/ipfs/$cid")
        // Regular IPFS URL with 2 /ipfs/ parts
        assertOriginalIpfsUrl("https://ipfs.io/ipfs/something/ipfs/$cid")
        // Regular IPFS URL but without CID
        assertOriginalIpfsUrl("http://ipfs.io/ipfs/123.jpg")
    }

    @Test
    fun `prefixed ipfs urls`() {
        assertFixedIpfsUrl("ipfs:/folder/$cid/abc .json", "folder/$cid/abc%20.json")
        assertFixedIpfsUrl("ipfs://folder/abc", "folder/abc")
        assertFixedIpfsUrl("ipfs:///folder/subfolder/$cid", "folder/subfolder/$cid")
        assertFixedIpfsUrl("ipfs:////$cid", cid)

        // Various case of ipfs prefix
        assertFixedIpfsUrl("IPFS://$cid", cid)
        assertFixedIpfsUrl("Ipfs:///$cid", cid)

        // Abstract IPFS urls with /ipfs/ path and broken slashes without a CID
        assertFixedIpfsUrl("ipfs:/ipfs/abc", "abc")
        assertFixedIpfsUrl("ipfs://ipfs/folder/abc", "folder/abc")
        assertFixedIpfsUrl("ipfs:///ipfs/abc", "abc")
    }

    @Test
    fun `single sid`() {
        assertFixedIpfsUrl(cid, cid)
    }

    @Test
    fun `regular url`() {
        val https = "https://api.t-o-s.xyz/ipfs/gucci/8.gif"
        val http = "http://api.guccinfts.xyz/ipfs/8"

        assertThat(service.resolvePublicHttpUrl(http)).isEqualTo(http)
        assertThat(service.resolvePublicHttpUrl(https)).isEqualTo(https)
    }

    @Test
    fun `some ipfs path path`() {
        val path = "///hel lo.png"
        assertThat(service.resolvePublicHttpUrl(path)).isEqualTo("${service.publicGateway}/hel%20lo.png")
    }

    private fun assertFixedIpfsUrl(url: String, expectedPath: String) {
        val result = ipfsService.resolvePublicHttpUrl(url)
        assertThat(result).isEqualTo("${service.publicGateway}/ipfs/$expectedPath")
    }

    private fun assertOriginalIpfsUrl(url: String, expectedPath: String? = null) {
        val expected = expectedPath ?: url // in most cases we expect URL not changed
        val result = ipfsService.resolvePublicHttpUrl(url)
        assertThat(result).isEqualTo(expected)
    }
}
