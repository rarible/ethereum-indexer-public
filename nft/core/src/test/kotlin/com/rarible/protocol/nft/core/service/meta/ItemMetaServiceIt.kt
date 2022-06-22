package com.rarible.protocol.nft.core.service.meta

import com.rarible.core.test.wait.Wait
import com.rarible.protocol.nft.core.data.createRandomItemId
import com.rarible.protocol.nft.core.data.createRandomUrl
import com.rarible.protocol.nft.core.data.randomItemMeta
import com.rarible.protocol.nft.core.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.core.integration.IntegrationTest
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@FlowPreview
@IntegrationTest
class ItemMetaServiceIt : AbstractIntegrationTest() {

    @Test
    fun `load and wait`() = runBlocking<Unit> {
        val itemMeta = randomItemMeta()
        val itemId = createRandomItemId()
        coEvery { mockItemMetaResolver.resolveItemMeta(itemId) } coAnswers {
            delay(300)
            itemMeta
        }
        assertThat(
            itemMetaService.getAvailableMeta(
                itemId = itemId,
                demander = "test"
            )
        ).isEqualTo(itemMeta)
        Wait.waitAssert {
            coVerify(exactly = 1) { mockItemMetaResolver.resolveItemMeta(itemId) }
        }
    }

    @Test
    fun `load synchronously`() = runBlocking<Unit> {
        val itemMeta = randomItemMeta()
        val itemId = createRandomItemId()
        coEvery { mockItemMetaResolver.resolveItemMeta(itemId) } coAnswers {
            delay(1000L)
            itemMeta
        }
        assertThat(
            itemMetaService.getAvailableMeta(
                itemId = itemId,
                demander = "test"
            )
        ).isEqualTo(itemMeta)
    }

    @Test
    fun `load synchronously - return null if loading has failed`() = runBlocking<Unit> {
        val itemId = createRandomItemId()
        val error = RuntimeException("loading-error")
        coEvery { mockItemMetaResolver.resolveItemMeta(itemId) } throws error

        assertThat(
            itemMetaService.getAvailableMeta(
                itemId = itemId,
                demander = "test"
            )
        ).isNull()
    }

    @Disabled //TODO: Need fix tests
    @Test
    fun `resolve meta for a pending item`() = runBlocking<Unit> {
        val itemMeta = randomItemMeta()
        val itemId = createRandomItemId()
        val tokenUri = createRandomUrl()
        coEvery { mockItemMetaResolver.resolvePendingItemMeta(itemId, tokenUri) } returns itemMeta
        itemMetaService.loadAndSavePendingItemMeta(itemId, tokenUri)
        assertThat(itemMetaService.getAvailableMeta(itemId,  "test")).isEqualTo(itemMeta)
        coVerify(exactly = 1) { mockItemMetaResolver.resolvePendingItemMeta(itemId, tokenUri) }
        coVerify(exactly = 0) { mockItemMetaResolver.resolveItemMeta(itemId) }
    }

}
