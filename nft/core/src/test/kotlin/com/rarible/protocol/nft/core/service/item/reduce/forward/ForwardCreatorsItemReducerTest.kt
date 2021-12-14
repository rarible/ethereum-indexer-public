package com.rarible.protocol.nft.core.service.item.reduce.forward

import com.rarible.core.test.data.randomAddress
import com.rarible.protocol.nft.core.data.createRandomCreatorsItemEvent
import com.rarible.protocol.nft.core.data.createRandomItem
import com.rarible.protocol.nft.core.data.createRandomMintItemEvent
import com.rarible.protocol.nft.core.model.Part
import com.rarible.protocol.nft.core.service.item.ItemCreatorService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono

internal class ForwardCreatorsItemReducerTest {
    private val creatorService = mockk<ItemCreatorService>()
    private val forwardCreatorsItemReducer = ForwardCreatorsItemReducer(creatorService)

    @Test
    fun `should get creators form creators event`() = runBlocking<Unit> {
        val creators = listOf(Part.fullPart(randomAddress()), Part.fullPart(randomAddress()))
        val event = createRandomCreatorsItemEvent().copy(creators = creators)
        val item = createRandomItem().copy(creators = emptyList(), creatorsFinal = false)

        coEvery { creatorService.getCreator(item.id) } returns Mono.empty()

        val reducedItem = forwardCreatorsItemReducer.reduce(item, event)
        assertThat(reducedItem.creators).isEqualTo(creators)
        assertThat(reducedItem.creatorsFinal).isTrue()
    }

    @Test
    fun `should get creators from creators service`() = runBlocking<Unit> {
        val serviceCreators = listOf(Part.fullPart(randomAddress()))
        val eventCreators = listOf(Part.fullPart(randomAddress()))
        val event = createRandomCreatorsItemEvent().copy(creators = eventCreators)
        val item = createRandomItem().copy(creators = emptyList(), creatorsFinal = false)

        coEvery { creatorService.getCreator(item.id) } returns Mono.just(serviceCreators.single().account)

        val reducedItem = forwardCreatorsItemReducer.reduce(item, event)
        assertThat(reducedItem.creators).isEqualTo(serviceCreators)
        assertThat(reducedItem.creatorsFinal).isTrue()
    }

    @Test
    fun `should get creator as a minter`() = runBlocking<Unit> {
        val owner = listOf(Part.fullPart(randomAddress()))
        val event = createRandomMintItemEvent().copy(owner = owner.single().account)
        val item = createRandomItem().copy(creators = emptyList(), creatorsFinal = false)

        coEvery { creatorService.getCreator(item.id) } returns Mono.empty()

        val reducedItem = forwardCreatorsItemReducer.reduce(item, event)
        assertThat(reducedItem.creators).isEqualTo(owner)
        assertThat(reducedItem.creatorsFinal).isFalse()
    }

    @Test
    fun `should not get creator as a minter`() = runBlocking<Unit> {
        val owner = listOf(Part.fullPart(randomAddress()))
        val finalCreators = listOf(Part.fullPart(randomAddress()))
        val event = createRandomMintItemEvent().copy(owner = owner.single().account)
        val item = createRandomItem().copy(creators = finalCreators, creatorsFinal = true)

        coEvery { creatorService.getCreator(item.id) } returns Mono.empty()

        val reducedItem = forwardCreatorsItemReducer.reduce(item, event)
        assertThat(reducedItem.creators).isEqualTo(finalCreators)
        assertThat(reducedItem.creatorsFinal).isTrue()
    }

    @Test
    fun `should get creators from creators service for mint event`() = runBlocking<Unit> {
        val serviceCreators = listOf(Part.fullPart(randomAddress()))
        val event = createRandomMintItemEvent()
        val item = createRandomItem().copy(creators = emptyList(), creatorsFinal = false)

        coEvery { creatorService.getCreator(item.id) } returns Mono.just(serviceCreators.single().account)

        val reducedItem = forwardCreatorsItemReducer.reduce(item, event)
        assertThat(reducedItem.creators).isEqualTo(serviceCreators)
        assertThat(reducedItem.creatorsFinal).isFalse()
    }
}
