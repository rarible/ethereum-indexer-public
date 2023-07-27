package com.rarible.protocol.nft.core.service.composit

import com.rarible.core.entity.reducer.service.EntityService
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.converters.model.ItemEventConverter
import com.rarible.protocol.nft.core.data.createRandomItem
import com.rarible.protocol.nft.core.data.createRandomItemId
import com.rarible.protocol.nft.core.data.createRandomMintItemEvent
import com.rarible.protocol.nft.core.data.createRandomOwnership
import com.rarible.protocol.nft.core.data.createRandomOwnershipTransferFromEvent
import com.rarible.protocol.nft.core.data.createRandomOwnershipTransferToEvent
import com.rarible.protocol.nft.core.model.CompositeEntity
import com.rarible.protocol.nft.core.model.CompositeEvent
import com.rarible.protocol.nft.core.model.ItemId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class ConsistencyCorrectorEntityServiceTest {
    private val delegate = mockk<EntityService<ItemId, CompositeEntity, CompositeEvent>>()
    private val reducer = mockk<CompositeReducer>()
    private val itemEventConverter = mockk<ItemEventConverter>()

    private val entityService = ConsistencyCorrectorEntityService(
        delegate = delegate,
        reducer = reducer,
        itemEventConverter = itemEventConverter,
    )

    @Test
    fun `should fix null item`() = runBlocking<Unit> {
        val itemId = createRandomItemId()
        val mintEvent = createRandomMintItemEvent()
        val ownershipTransferToEvent = createRandomOwnershipTransferToEvent()
        val ownershipTransferFromEvent = createRandomOwnershipTransferFromEvent()
        val firstEvent = CompositeEvent(listOf(ownershipTransferFromEvent, ownershipTransferToEvent))
        val ownership1 = createRandomOwnership().copy(value = EthUInt256.of(2))
        val ownership2 = createRandomOwnership().copy(value = EthUInt256.of(3))
        val expectedItem = createRandomItem().copy(supply = EthUInt256.of(5))
        val invalidEntity = CompositeEntity(
            id = itemId,
            item = null,
            ownerships = mutableMapOf(ownership1.owner to ownership1, ownership2.owner to ownership2),
            firstEvent = firstEvent
        )
        every { itemEventConverter.convertToMintEvent(ownershipTransferToEvent) } returns mintEvent
        coEvery { reducer.reduce(any(), any()) } returns CompositeEntity(expectedItem.copy(supply = EthUInt256.ZERO))
        coEvery { delegate.update(any()) } answers { firstArg() }

        val fixesEntity = entityService.update(invalidEntity)

        assertThat(fixesEntity.id).isEqualTo(invalidEntity.id)
        assertThat(fixesEntity.item).isEqualTo(expectedItem)
        assertThat(fixesEntity.ownerships).isEqualTo(invalidEntity.ownerships)
        assertThat(fixesEntity.firstEvent).isEqualTo(invalidEntity.firstEvent)
        coVerify { reducer.reduce(
            withArg {
                assertThat(it.id).isEqualTo(itemId)
                assertThat(it.item).isNull()
                assertThat(it.ownerships).hasSize(0)
                assertThat(it.firstEvent).isNull()
            },
            withArg {
                assertThat(it).isEqualTo(CompositeEvent(mintEvent))
            })
        }
    }

    @Test
    fun `should fix item supply`() = runBlocking<Unit> {
        val item = createRandomItem()
        val ownership1 = createRandomOwnership().copy(value = EthUInt256.of(2))
        val ownership2 = createRandomOwnership().copy(value = EthUInt256.of(3))
        val expectedItem = item.copy(supply = EthUInt256.of(5))
        val invalidEntity = CompositeEntity(
            id = item.id,
            item = item,
            ownerships = mutableMapOf(ownership1.owner to ownership1, ownership2.owner to ownership2),
            firstEvent = null
        )
        coEvery { delegate.update(any()) } answers { firstArg() }

        val fixesEntity = entityService.update(invalidEntity)

        assertThat(fixesEntity.id).isEqualTo(invalidEntity.id)
        assertThat(fixesEntity.item).isEqualTo(expectedItem)
        assertThat(fixesEntity.ownerships).isEqualTo(invalidEntity.ownerships)
        assertThat(fixesEntity.firstEvent).isEqualTo(invalidEntity.firstEvent)
        verify(exactly = 0) { itemEventConverter.convertToMintEvent(any()) }
        coVerify(exactly = 0) { reducer.reduce(any(), any()) }
    }
}
