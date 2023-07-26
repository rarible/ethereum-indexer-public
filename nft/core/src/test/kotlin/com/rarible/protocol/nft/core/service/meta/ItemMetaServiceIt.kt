package com.rarible.protocol.nft.core.service.meta

import com.rarible.core.test.wait.Wait
import com.rarible.protocol.nft.core.data.createRandomItemId
import com.rarible.protocol.nft.core.data.randomItemMeta
import com.rarible.protocol.nft.core.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.core.integration.IntegrationTest
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

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

        val result = itemMetaService.getMeta(itemId)
        assertThat(result).isEqualTo(itemMeta)

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

        val result = itemMetaService.getMeta(itemId)
        assertThat(result).isEqualTo(itemMeta)
    }

    @Test
    fun `load synchronously - return null if loading has failed`() = runBlocking<Unit> {
        val itemId = createRandomItemId()
        val error = RuntimeException("loading-error")
        coEvery { mockItemMetaResolver.resolveItemMeta(itemId) } throws error

        assertThrows<RuntimeException> {
            itemMetaService.getMeta(itemId)
        }
    }
}
