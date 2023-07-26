package com.rarible.protocol.nft.listener.service.suspicios

import com.rarible.core.test.data.randomBoolean
import com.rarible.protocol.nft.core.data.randomItemExState
import com.rarible.protocol.nft.core.data.randomUpdateSuspiciousItemsStateAsset
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.repository.data.createItem
import com.rarible.protocol.nft.core.repository.item.ItemExStateRepository
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import com.rarible.protocol.nft.core.service.item.reduce.ItemUpdateService
import com.rarible.protocol.nft.listener.metrics.NftListenerMetricsFactory
import com.rarible.protocol.nft.listener.service.opensea.OpenSeaService
import com.rarible.protocol.nft.listener.test.data.randomOpenSeaAsset
import com.rarible.protocol.nft.listener.test.data.randomOpenSeaAssets
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import reactor.core.publisher.Mono

@Suppress("ReactiveStreamsUnusedPublisher")
internal class SuspiciousItemsServiceTest {
    private val itemRepository = mockk<ItemRepository>()
    private val itemStateRepository = mockk<ItemExStateRepository>()
    private val openSeaService = mockk<OpenSeaService>()
    private val itemUpdateService = mockk<ItemUpdateService>()
    private val listenerMetrics = mockk<NftListenerMetricsFactory> {
        every { onSuspiciousItemUpdate() } returns Unit
        every { onSuspiciousItemsGet(any()) } returns Unit
        every { onSuspiciousItemFound() } returns Unit
    }
    private val service = SuspiciousItemsService(
        itemRepository,
        itemStateRepository,
        openSeaService,
        itemUpdateService,
        listenerMetrics
    )

    @BeforeEach
    fun cleanMocks() {
        clearMocks(itemRepository, itemStateRepository, openSeaService, itemUpdateService)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `update - with existed state`(exStateExist: Boolean) = runBlocking<Unit> {
        val supportsWyvern = randomBoolean()
        val expectedSuspicious = supportsWyvern.not()

        val asset = randomOpenSeaAsset().copy(supportsWyvern = supportsWyvern)
        val itemId = ItemId(asset.assetContract.address, asset.tokenId)
        val item = createItem(itemId).copy(isSuspiciousOnOS = expectedSuspicious.not())
        val savedExState = if (exStateExist) randomItemExState(itemId) else null

        val openSeaItems = randomOpenSeaAssets(listOf(asset))

        val stateAsset = randomUpdateSuspiciousItemsStateAsset()
        every { itemRepository.findById(itemId) } returns Mono.just(item)
        coEvery { openSeaService.getOpenSeaAssets(stateAsset.contract, stateAsset.cursor) } returns openSeaItems
        coEvery { itemUpdateService.update(any()) } returns item
        coEvery { itemStateRepository.getById(itemId) } returns savedExState
        coEvery { itemStateRepository.save(any()) } returns randomItemExState(itemId)

        service.update(stateAsset)

        coVerify(exactly = 1) {
            itemStateRepository.save(withArg {
                assertThat(it.id).isEqualTo(itemId)
                assertThat(it.isSuspiciousOnOS).isEqualTo(expectedSuspicious)
            })
            itemUpdateService.update(withArg {
                assertThat(it.id).isEqualTo(itemId)
                assertThat(it.isSuspiciousOnOS).isEqualTo(expectedSuspicious)
            })
        }
    }

    @Test
    fun `no update`() = runBlocking<Unit> {
        val supportsWyvern = randomBoolean()
        val expectedSuspicious = supportsWyvern.not()

        val asset = randomOpenSeaAsset().copy(supportsWyvern = supportsWyvern)
        val itemId = ItemId(asset.assetContract.address, asset.tokenId)
        val item = createItem(itemId).copy(isSuspiciousOnOS = expectedSuspicious)

        val openSeaItems = randomOpenSeaAssets(listOf(asset))

        val stateAsset = randomUpdateSuspiciousItemsStateAsset()
        every { itemRepository.findById(itemId) } returns Mono.just(item)
        coEvery { openSeaService.getOpenSeaAssets(stateAsset.contract, stateAsset.cursor) } returns openSeaItems

        service.update(stateAsset)

        coVerify(exactly = 0) {
            itemStateRepository.save(any())
            itemUpdateService.update(any())
        }
    }
}
