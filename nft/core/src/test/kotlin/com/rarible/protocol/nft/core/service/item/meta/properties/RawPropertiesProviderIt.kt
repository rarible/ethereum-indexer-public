package com.rarible.protocol.nft.core.service.item.meta.properties

import com.rarible.core.common.nowMillis
import com.rarible.core.meta.resource.parser.UrlParser
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomString
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.core.integration.IntegrationTest
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.service.item.meta.cache.ContentCacheStorage
import com.rarible.protocol.nft.core.service.item.meta.cache.MetaRawPropertiesEntry
import io.mockk.coEvery
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
class RawPropertiesProviderIt : AbstractIntegrationTest() {

    @Autowired
    lateinit var rawPropertiesProvider: RawPropertiesProvider

    @Autowired
    lateinit var contentCacheStorage: ContentCacheStorage<MetaRawPropertiesEntry>

    @Autowired
    lateinit var urlParser: UrlParser

    private val cid = "QmeqeBpsYTuJL8AZhY9fGBeTj9QuvMVqaZeRWFnjA24QEE"

    private val itemId = ItemId(randomAddress(), EthUInt256.of(3709))

    @BeforeEach
    fun turnOnFeatureFlag() {
        featureFlags.enableMetaRawPropertiesCache = true
    }

    @AfterEach
    fun turnOffFeatureFlag() {
        featureFlags.enableMetaRawPropertiesCache = false
    }

    @Test
    fun `cacheable url - cached`() = runBlocking<Unit> {
        val path = "$cid/${randomString()}"
        val urlResource = urlParser.parse("https://ipfs.io/ipfs/$path")!!
        val rawProperties = "rawProperties"

        coEvery { mockExternalHttpClient.getBody(url = any(), id = itemId.toString()) } returns rawProperties

        val properties = rawPropertiesProvider.getContent(itemId, urlResource)

        val fromCache = contentCacheStorage.get("ipfs://$path")!!

        // Content returned and cached
        assertThat(properties).isEqualTo(fromCache.content)
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
}
