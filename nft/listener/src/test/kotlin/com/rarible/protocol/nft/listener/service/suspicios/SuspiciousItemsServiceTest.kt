package com.rarible.protocol.nft.listener.service.suspicios

import com.rarible.protocol.nft.core.data.randomItemExState
import com.rarible.protocol.nft.core.data.randomUpdateSuspiciousItemsStateAsset
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.repository.data.createItem
import com.rarible.protocol.nft.core.repository.item.ItemExStateRepository
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import com.rarible.protocol.nft.core.service.item.reduce.ItemUpdateService
import com.rarible.protocol.nft.listener.service.opensea.OpenSeaService
import com.rarible.protocol.nft.listener.test.data.randomOpenSeaAsset
import com.rarible.protocol.nft.listener.test.data.randomOpenSeaAssets
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono

@Suppress("ReactiveStreamsUnusedPublisher")
internal class SuspiciousItemsServiceTest {
    private val itemRepository = mockk<ItemRepository>()
    private val itemStateRepository = mockk<ItemExStateRepository>()
    private val openSeaService = mockk<OpenSeaService>()
    private val itemUpdateService = mockk<ItemUpdateService>()
    private val service = SuspiciousItemsService(itemRepository, itemStateRepository, openSeaService, itemUpdateService)

    @Test
    fun update() = runBlocking<Unit> {
        val alreadyUpdated = randomOpenSeaAsset()
        val alreadyUpdatedItemId = ItemId(alreadyUpdated.assetContract.address, alreadyUpdated.tokenId)
        val alreadyUpdatedItem = createItem(alreadyUpdatedItemId).copy(isSuspiciousOnOS = alreadyUpdated.supportsWyvern)

        val needUpdated = randomOpenSeaAsset()
        val needUpdatedItemId = ItemId(needUpdated.assetContract.address, needUpdated.tokenId)
        val needUpdatedItem = createItem(needUpdatedItemId).copy(isSuspiciousOnOS = needUpdated.supportsWyvern.not())

        val openSeaItems = randomOpenSeaAssets(listOf(needUpdated, alreadyUpdated) )

        val stateAsset = randomUpdateSuspiciousItemsStateAsset()
        coEvery { openSeaService.getOpenSeaAssets(stateAsset.contract, stateAsset.cursor) } returns openSeaItems
        every { itemRepository.findById(needUpdatedItemId) } returns Mono.just(needUpdatedItem)
        every { itemRepository.findById(alreadyUpdatedItemId) } returns Mono.just(alreadyUpdatedItem)

        coEvery { itemUpdateService.update(any()) } returns needUpdatedItem
        coEvery { itemStateRepository.getById(needUpdatedItemId) } returns randomItemExState(needUpdatedItemId)
        coEvery { itemStateRepository.save(any()) } returns randomItemExState(needUpdatedItemId)

        service.update(stateAsset)

        coVerify(exactly = 1) {
            itemUpdateService.update(any())

            itemUpdateService.update(withArg {
                assertThat(it.id).isEqualTo(needUpdatedItemId)
                assertThat(it.isSuspiciousOnOS).isEqualTo(needUpdated.supportsWyvern)
            })
        }

        coVerify(exactly = 1) {
            itemStateRepository.save(any())
            itemStateRepository.getById(needUpdatedItemId)

            itemStateRepository.save(withArg {
                assertThat(it.id).isEqualTo(needUpdatedItemId)
                assertThat(it.isSuspiciousOnOS).isEqualTo(needUpdated.supportsWyvern)
            })
        }
    }
}