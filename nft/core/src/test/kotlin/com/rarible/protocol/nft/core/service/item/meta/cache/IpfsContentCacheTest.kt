package com.rarible.protocol.nft.core.service.item.meta.cache

import com.rarible.core.meta.resource.model.HttpUrl
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class IpfsContentCacheTest {

    private val ipfsContentCache = IpfsContentCache(mockk())

    @Test
    fun `unsupported resource`() = runBlocking<Unit> {
        val resource = HttpUrl("http://localhost:8080")
        assertThrows<ContentCacheException> {
            ipfsContentCache.save(resource, "content")
        }
    }
}
