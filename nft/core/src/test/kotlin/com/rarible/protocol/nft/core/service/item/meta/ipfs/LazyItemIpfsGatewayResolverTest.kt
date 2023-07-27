package com.rarible.protocol.nft.core.service.item.meta.ipfs

import com.rarible.core.meta.resource.model.IpfsUrl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class LazyItemIpfsGatewayResolverTest {

    private val lazyIpfsGateway = "http://lazygateway.com"
    private val ipfsKey = "QmbpJhWFiwzNu7MebvKG3hrYiyWmSiz5dTUYMQLXsjT9vw"
    private val ipfsPath = "ipfs/$ipfsKey"
    private val resolver = LazyItemIpfsGatewayResolver(lazyIpfsGateway)

    @Test
    fun `get url - replaced`() {
        val ipfsUrl = IpfsUrl("$lazyIpfsGateway/$ipfsPath", lazyIpfsGateway, ipfsPath)
        val result = resolver.getResourceUrl(ipfsUrl, "", true)

        assertThat(result).isEqualTo(ipfsUrl.original)
    }

    @Test
    fun `get url - skipped, no gateway`() {
        val ipfsUrl = IpfsUrl("/$ipfsPath", null, ipfsPath)
        val result = resolver.getResourceUrl(ipfsUrl, "", true)

        assertThat(result).isNull()
    }

    @Test
    fun `get url - skipped, replacement not required`() {
        val ipfsUrl = IpfsUrl("$lazyIpfsGateway/$ipfsPath", lazyIpfsGateway, ipfsPath)
        val result = resolver.getResourceUrl(ipfsUrl, "", false)

        assertThat(result).isNull()
    }
}
