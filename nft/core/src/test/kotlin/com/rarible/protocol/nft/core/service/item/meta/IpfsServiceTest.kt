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
    fun `foreign ipfs urls`() {
        // Regular IPFS URL
        assertIpfsUrl("https://ipfs.io/ipfs/$cid", cid)
        // Regular IPFS URL with 2 /ipfs/ parts
        assertIpfsUrl("https://ipfs.io/ipfs/something/ipfs/$cid", cid)
        // Broken IPFS URL
        assertIpfsUrl("https://mypinata.com/ipfs/http://ipfs.io/ipfs/$cid", cid)
        // Relative IPFS path
        assertIpfsUrl("/ipfs/$cid/abc .png", "$cid/abc%20.png")

        // Abstract IPFS urls with /ipfs/ path and broken slashes
        assertIpfsUrl("ipfs:/ipfs/$cid", cid)
        assertIpfsUrl("ipfs://ipfs/$cid", cid)
        assertIpfsUrl("ipfs:///ipfs/$cid", cid)
        assertIpfsUrl("ipfs:////ipfs/$cid", cid)

        assertIpfsUrl("ipfs:////ipfs/$cid", cid)
        assertIpfsUrl("ipfs:////ipfs//$cid", cid)
        assertIpfsUrl("ipfs:////ipfs///$cid", cid)

        // Regular IPFS URL but without CID, should stay as is
        val publicUrlWithoutCid = "https://ipfs.io/ipfs/123.jpg"
        assertThat(service.resolvePublicHttpUrl(publicUrlWithoutCid)).isEqualTo(publicUrlWithoutCid)
    }

    @Test
    fun `prefixed ipfs urls`() {
        assertIpfsUrl("ipfs:/folder/$cid/abc .json", "folder/$cid/abc%20.json")
        assertIpfsUrl("ipfs://folder/abc", "folder/abc")
        assertIpfsUrl("ipfs:///folder/subfolder/$cid", "folder/subfolder/$cid")
        assertIpfsUrl("ipfs:////$cid", cid)

        // Various case of ipfs prefix
        assertIpfsUrl("IPFS://$cid", cid)
        assertIpfsUrl("Ipfs:///$cid", cid)

        // Abstract IPFS urls with /ipfs/ path and broken slashes without a CID
        assertIpfsUrl("ipfs:/ipfs/abc", "abc")
        assertIpfsUrl("ipfs://ipfs/folder/abc", "folder/abc")
        assertIpfsUrl("ipfs:///ipfs/abc", "abc")
    }

    @Test
    fun `single sid`() {
        assertIpfsUrl(cid, cid)
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

    private fun assertIpfsUrl(url: String, expectedPath: String) {
        val result = ipfsService.resolvePublicHttpUrl(url)
        assertThat(result).isEqualTo("${service.publicGateway}/ipfs/$expectedPath")
    }
}
