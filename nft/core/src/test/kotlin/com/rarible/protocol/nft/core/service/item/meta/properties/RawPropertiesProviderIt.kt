package com.rarible.protocol.nft.core.service.item.meta.properties

import com.rarible.core.common.nowMillis
import com.rarible.core.meta.resource.parser.UrlParser
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomString
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.FeatureFlags
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.service.UrlService
import com.rarible.protocol.nft.core.service.item.meta.cache.ContentCacheStorage
import com.rarible.protocol.nft.core.service.item.meta.cache.MetaRawPropertiesEntry
import com.rarible.protocol.nft.core.service.item.meta.cache.RawPropertiesCacheService
import com.rarible.protocol.nft.core.test.AbstractIntegrationTest
import com.rarible.protocol.nft.core.test.IntegrationTest
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.client.WebClientResponseException

@IntegrationTest
class RawPropertiesProviderIt : AbstractIntegrationTest() {

    @Autowired
    lateinit var rawPropertiesCacheService: RawPropertiesCacheService

    @Autowired
    lateinit var urlService: UrlService

    @Autowired
    lateinit var contentCacheStorage: ContentCacheStorage<MetaRawPropertiesEntry>

    @Autowired
    lateinit var urlParser: UrlParser

    private lateinit var rawPropertiesProvider: RawPropertiesProvider

    private val cid = "QmeqeBpsYTuJL8AZhY9fGBeTj9QuvMVqaZeRWFnjA24QEE"
    private val itemId = ItemId(randomAddress(), EthUInt256.of(3709))

    @BeforeEach
    fun beforeEach() {
        rawPropertiesProvider = createProvider(enableCache = true)
    }

    @Test
    fun `cacheable url - cached`() = runBlocking<Unit> {
        val path = "$cid/${randomString()}"
        val urlResource = urlParser.parse("https://ipfs.io/ipfs/$path")!!

        coEvery {
            mockExternalHttpClient.getBody(url = any(), id = itemId.toString(), useProxy = false)
        } returns "rawProperties"

        val properties = rawPropertiesProvider.getContent(itemId, urlResource)

        val fromCache = contentCacheStorage.get("ipfs://$path")!!

        // Content returned and cached
        assertThat(properties).isEqualTo(fromCache.content)
    }

    @Test
    fun `cacheable url - proxy not used`() = runBlocking<Unit> {
        rawPropertiesProvider = createProvider(enableCache = true, enableProxy = true)
        val path = "$cid/${randomString()}"
        val urlResource = urlParser.parse("https://ipfs.io/ipfs/$path")!!

        coEvery {
            mockExternalHttpClient.getBody(url = any(), id = itemId.toString(), useProxy = false)
        } returns "rawProperties"

        val properties = rawPropertiesProvider.getContent(itemId, urlResource)

        val fromCache = contentCacheStorage.get("ipfs://$path")!!

        // Content returned and cached
        assertThat(properties).isEqualTo(fromCache.content)
        // Proxy not used since we fetched data from IPFS
        coVerify(exactly = 1) { mockExternalHttpClient.getBody(url = any(), id = itemId.toString(), useProxy = false) }
    }

    @Test
    fun `cacheable url - cache disabled`() = runBlocking<Unit> {
        rawPropertiesProvider = createProvider(enableCache = false)
        val path = "$cid/${randomString()}"
        val urlResource = urlParser.parse("https://ipfs.io/ipfs/$path")!!
        val rawProperties = "rawProperties"

        coEvery { mockExternalHttpClient.getBody(url = any(), id = itemId.toString()) } returns rawProperties

        rawPropertiesProvider.getContent(itemId, urlResource)

        val fromCache = contentCacheStorage.get("ipfs://$path")

        // Should not be cached since cache is disabled
        assertThat(fromCache).isNull()
    }

    @Test
    fun `cacheable url - not cached, content is not empty`() = runBlocking<Unit> {
        val path = "$cid/${randomString()}"
        val urlResource = urlParser.parse("ipfs://$path")!!
        val rawProperties = ""

        coEvery { mockExternalHttpClient.getBody(url = any(), id = itemId.toString()) } returns rawProperties

        rawPropertiesProvider.getContent(itemId, urlResource)

        val fromCache = contentCacheStorage.get("ipfs://$path")

        assertThat(fromCache).isNull()
    }

    @Test
    fun `cacheable url - not cached, content is null`() = runBlocking<Unit> {
        val path = "$cid/${randomString()}"
        val urlResource = urlParser.parse("ipfs://$path")!!

        // Content not resolved
        coEvery { mockExternalHttpClient.getBody(url = any(), id = itemId.toString()) } returns null

        rawPropertiesProvider.getContent(itemId, urlResource)

        val fromCache = contentCacheStorage.get("ipfs://$path")

        assertThat(fromCache).isNull()
    }

    @Test
    fun `cacheable url - from cache`() = runBlocking<Unit> {
        val path = "$cid/${randomString()}"
        val urlResource = urlParser.parse(path)!!
        val rawProperties = "rawProperties"

        val entry = MetaRawPropertiesEntry(
            url = "ipfs://$path",
            updatedAt = nowMillis(),
            content = rawProperties
        )

        contentCacheStorage.save(entry)

        val properties = rawPropertiesProvider.getContent(itemId, urlResource)

        // Content returned and cached
        assertThat(properties).isEqualTo(entry.content)
    }

    @Test
    fun `not cacheable url`() = runBlocking<Unit> {
        val urlResource = urlParser.parse("https://localhost:8080/abc")!!
        val rawProperties = "rawProperties"

        coEvery { mockExternalHttpClient.getBody(url = any(), id = itemId.toString()) } returns rawProperties

        rawPropertiesProvider.getContent(itemId, urlResource)

        val fromCache = contentCacheStorage.get(urlResource.original)

        // Not cached
        assertThat(fromCache).isNull()
    }

    @Test
    fun `not cacheable url - proxy used`() = runBlocking<Unit> {
        rawPropertiesProvider = createProvider(enableCache = true, enableProxy = true)

        val urlResource = urlParser.parse("https://test.com/${randomString()}")!!

        // First call should be executed without proxy
        coEvery {
            mockExternalHttpClient.getBody(url = any(), id = itemId.toString(), useProxy = false)
        } throws WebClientResponseException(404, "", HttpHeaders(), null, null, null)

        // Since direct request has failed, proxy request should be executed
        coEvery {
            mockExternalHttpClient.getBody(url = any(), id = itemId.toString(), useProxy = true)
        } returns "rawProperties"

        rawPropertiesProvider.getContent(itemId, urlResource)

        // Content is not cached since it is not an IPFS URL
        coVerify(exactly = 1) { mockExternalHttpClient.getBody(url = any(), id = itemId.toString(), useProxy = false) }
        coVerify(exactly = 1) { mockExternalHttpClient.getBody(url = any(), id = itemId.toString(), useProxy = true) }
    }

    @Test
    fun `not cacheable url - proxy not used`() = runBlocking<Unit> {
        rawPropertiesProvider = createProvider(enableCache = true, enableProxy = true)

        val urlResource = urlParser.parse("https://test.com/${randomString()}")!!

        // First call should be executed without proxy - and it returns data
        coEvery {
            mockExternalHttpClient.getBody(url = any(), id = itemId.toString(), useProxy = false)
        } returns "rawProperties"

        rawPropertiesProvider.getContent(itemId, urlResource)

        // Even if useProxy == true, proxy should not be used since we got data via direct request
        coVerify(exactly = 1) { mockExternalHttpClient.getBody(url = any(), id = itemId.toString(), useProxy = false) }
        coVerify(exactly = 0) { mockExternalHttpClient.getBody(url = any(), id = itemId.toString(), useProxy = true) }
    }

    private fun createProvider(enableCache: Boolean = false, enableProxy: Boolean = false): RawPropertiesProvider {
        return RawPropertiesProvider(
            rawPropertiesCacheService,
            urlService,
            mockExternalHttpClient,
            FeatureFlags(
                enableMetaRawPropertiesCache = enableCache,
                enableProxyForMetaDownload = enableProxy
            )
        )
    }
}
