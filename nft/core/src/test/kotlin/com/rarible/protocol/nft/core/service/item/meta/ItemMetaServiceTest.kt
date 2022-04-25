package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.loader.cache.CacheLoaderService
import com.rarible.protocol.nft.core.data.createRandomItem
import com.rarible.protocol.nft.core.data.randomItemMeta
import com.rarible.protocol.nft.core.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.core.integration.IntegrationTest
import com.rarible.protocol.nft.core.model.ItemMeta
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import java.time.Duration

@IntegrationTest
internal class ItemMetaServiceTest : AbstractIntegrationTest() {

    @Qualifier("meta.cache.loader.service")
    @Autowired
    private lateinit var itemMetaCacheLoaderService: CacheLoaderService<ItemMeta>

    @Test
    fun `should get meta from cache`() = runBlocking<Unit> {
        val item = createRandomItem()
        val itemId = item.id
        val itemMeta = randomItemMeta()
        itemMetaCacheLoaderService.save(itemId.toCacheKey(), itemMeta)

        coVerify(exactly = 0) { mockItemMetaResolver.resolveItemMeta(itemId) }
        Assertions.assertThat(itemMetaService.getAvailableMeta(itemId, "test")).isEqualTo(itemMeta)
    }

    @Test
    fun `should always load meta`() = runBlocking<Unit> {
        val item = createRandomItem()
        val itemId = item.id
        val cachedItemMeta = randomItemMeta()
        val resolvedItemMeta = randomItemMeta()
        itemMetaCacheLoaderService.save(itemId.toCacheKey(), cachedItemMeta)

        coEvery { mockItemMetaResolver.resolveItemMeta(itemId) } returns resolvedItemMeta
        Assertions.assertThat(itemMetaService.getAvailableMetaOrLoadSynchronouslyWithTimeout(itemId, Duration.ofHours(1), "test")).isEqualTo(resolvedItemMeta)
    }
}