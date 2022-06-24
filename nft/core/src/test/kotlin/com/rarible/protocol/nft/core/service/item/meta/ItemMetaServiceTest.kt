package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.protocol.nft.core.data.createRandomItem
import com.rarible.protocol.nft.core.data.randomItemMeta
import com.rarible.protocol.nft.core.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.core.integration.IntegrationTest
import io.mockk.coEvery
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration

@IntegrationTest
internal class ItemMetaServiceTest : AbstractIntegrationTest() {

    @Test
    fun `should always load meta`() = runBlocking<Unit> {
        val item = createRandomItem()
        val itemId = item.id
        val resolvedItemMeta = randomItemMeta()

        coEvery { mockItemMetaResolver.resolveItemMeta(itemId) } returns resolvedItemMeta
        assertThat(itemMetaService.getMetaWithTimeout(itemId, Duration.ofHours(1), "test")).isEqualTo(resolvedItemMeta)
    }
}
