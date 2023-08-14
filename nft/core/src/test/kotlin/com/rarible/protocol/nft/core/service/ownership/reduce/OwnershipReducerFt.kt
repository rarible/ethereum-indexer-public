package com.rarible.protocol.nft.core.service.ownership.reduce

import com.rarible.blockchain.scanner.ethereum.model.EthereumBlockStatus
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.data.createRandomOwnership
import com.rarible.protocol.nft.core.data.createRandomOwnershipId
import com.rarible.protocol.nft.core.data.createRandomOwnershipTransferFromEvent
import com.rarible.protocol.nft.core.data.createRandomOwnershipTransferToEvent
import com.rarible.protocol.nft.core.data.withNewValues
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.OwnershipEvent
import com.rarible.protocol.nft.core.test.AbstractIntegrationTest
import com.rarible.protocol.nft.core.test.IntegrationTest
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@FlowPreview
@IntegrationTest
internal class OwnershipReducerFt : AbstractIntegrationTest() {
    @Autowired
    private lateinit var ownershipReducer: OwnershipReducer

    @Autowired
    private lateinit var ownershipTemplateProvider: OwnershipTemplateProvider

    @Test
    fun `should handle simple transfer to event`() = runBlocking<Unit> {
        val ownership = initial()

        val transferTo = createRandomOwnershipTransferToEvent()
            .withNewValues(status = EthereumBlockStatus.CONFIRMED, blockNumber = 1)
            .copy(value = EthUInt256.ONE)

        val reducedOwnership = reduce(ownership, transferTo)
        assertThat(reducedOwnership.value).isEqualTo(EthUInt256.ONE)
        assertThat(reducedOwnership.revertableEvents).containsExactlyElementsOf(listOf(transferTo))
        assertThat(reducedOwnership.deleted).isFalse()
    }

    @Test
    fun `should handle simple transfer from event`() = runBlocking<Unit> {
        val ownership = initial().copy(value = EthUInt256.TEN)

        val transferFrom = createRandomOwnershipTransferFromEvent()
            .withNewValues(status = EthereumBlockStatus.CONFIRMED, blockNumber = 1)
            .copy(value = EthUInt256.ONE)

        val reducedOwnership = reduce(ownership, transferFrom)
        assertThat(reducedOwnership.value).isEqualTo(EthUInt256.of(9))
        assertThat(reducedOwnership.revertableEvents).containsExactlyElementsOf(listOf(transferFrom))
        assertThat(reducedOwnership.deleted).isFalse()
        assertThat(reducedOwnership.date).isEqualTo(transferFrom.log.createdAt)
    }

    @Test
    fun `should handle multy transfers`() = runBlocking<Unit> {
        val ownership = initial().copy(value = EthUInt256.TEN)

        val events = listOf(
            createRandomOwnershipTransferFromEvent()
                .withNewValues(status = EthereumBlockStatus.CONFIRMED, blockNumber = 1)
                .copy(value = EthUInt256.of(2)),
            createRandomOwnershipTransferFromEvent()
                .withNewValues(status = EthereumBlockStatus.CONFIRMED, blockNumber = 2)
                .copy(value = EthUInt256.of(5)),
            createRandomOwnershipTransferToEvent()
                .withNewValues(status = EthereumBlockStatus.CONFIRMED, blockNumber = 3)
                .copy(value = EthUInt256.ONE),
            createRandomOwnershipTransferToEvent()
                .withNewValues(status = EthereumBlockStatus.CONFIRMED, blockNumber = 4)
                .copy(value = EthUInt256.ONE)
        )
        val reducedOwnership = reduce(ownership, events)
        assertThat(reducedOwnership.value).isEqualTo(EthUInt256.of(5))
        assertThat(reducedOwnership.revertableEvents).containsExactlyElementsOf(events)
        assertThat(reducedOwnership.deleted).isFalse()
        assertThat(reducedOwnership.date).isEqualTo(events.last().log.createdAt)
    }

    @Test
    fun `should not handle applied events`() = runBlocking<Unit> {
        val ownership = initial().copy(value = EthUInt256.TEN)

        val event1 = createRandomOwnershipTransferFromEvent()
            .withNewValues(status = EthereumBlockStatus.CONFIRMED, blockNumber = 1)
            .copy(value = EthUInt256.of(2))
        val event2 = createRandomOwnershipTransferFromEvent()
            .withNewValues(status = EthereumBlockStatus.CONFIRMED, blockNumber = 2, logIndex = 1, minorLogIndex = 2)
            .copy(value = EthUInt256.of(5))
        val duplicate1 = createRandomOwnershipTransferFromEvent()
            .withNewValues(status = EthereumBlockStatus.CONFIRMED, blockNumber = 2, logIndex = 1, minorLogIndex = 2)
            .copy(value = EthUInt256.of(5))
        val event3 = createRandomOwnershipTransferToEvent()
            .withNewValues(status = EthereumBlockStatus.CONFIRMED, blockNumber = 3, logIndex = 10, minorLogIndex = 0)
            .copy(value = EthUInt256.ONE)
        val duplicate2 = createRandomOwnershipTransferToEvent()
            .withNewValues(status = EthereumBlockStatus.CONFIRMED, blockNumber = 3, logIndex = 10, minorLogIndex = 0)
            .copy(value = EthUInt256.ONE)
        val event4 = createRandomOwnershipTransferToEvent()
            .withNewValues(status = EthereumBlockStatus.CONFIRMED, blockNumber = 4)
            .copy(value = EthUInt256.ONE)
        val duplicate3 = createRandomOwnershipTransferFromEvent()
            .withNewValues(status = EthereumBlockStatus.CONFIRMED, blockNumber = 2, logIndex = 1, minorLogIndex = 2)
            .copy(value = EthUInt256.of(5))

        val reducedOwnership = reduce(ownership, event1, event2, duplicate1, event3, duplicate2, event4, duplicate3)
        assertThat(reducedOwnership.value).isEqualTo(EthUInt256.of(5))
        assertThat(reducedOwnership.revertableEvents).containsExactlyElementsOf(
            listOf(event1, event2, event3, event4)
        )
        assertThat(reducedOwnership.deleted).isFalse()
    }

    @Test
    fun `should has only needed confirm events in the revertableEvents`() = runBlocking<Unit> {
        val ownership = initial().copy(value = EthUInt256.TEN)

        val notRevertable1 = createRandomOwnershipTransferFromEvent()
            .withNewValues(status = EthereumBlockStatus.CONFIRMED, blockNumber = 1)
        val notRevertable2 = createRandomOwnershipTransferFromEvent()
            .withNewValues(status = EthereumBlockStatus.CONFIRMED, blockNumber = 2)
        val notRevertable3 = createRandomOwnershipTransferToEvent()
            .withNewValues(status = EthereumBlockStatus.CONFIRMED, blockNumber = 3)
        val revertable1 = createRandomOwnershipTransferToEvent()
            .withNewValues(status = EthereumBlockStatus.CONFIRMED, blockNumber = 14)
        val revertable2 = createRandomOwnershipTransferToEvent()
            .withNewValues(status = EthereumBlockStatus.CONFIRMED, blockNumber = 15)
        val revertable3 = createRandomOwnershipTransferToEvent()
            .withNewValues(status = EthereumBlockStatus.CONFIRMED, blockNumber = 16)

        val reducedOwnership = reduce(
            ownership, notRevertable1, notRevertable2, notRevertable3, revertable1, revertable2, revertable3
        )
        assertThat(reducedOwnership.revertableEvents).containsExactlyElementsOf(listOf(
            notRevertable3, revertable1, revertable2, revertable3
        ))
    }

    @Test
    fun `revert compacted events`() = runBlocking<Unit> {
        val ownership = createRandomOwnership().copy(
            lazyValue = EthUInt256.ZERO,
            value = EthUInt256.ZERO,
            deleted = true,
            revertableEvents = emptyList()
        )
        val block1 = (1..101).map {
            createRandomOwnershipTransferToEvent()
                .withNewValues(EthereumBlockStatus.CONFIRMED, blockNumber = 1, logIndex = it)
                .copy(value = EthUInt256.of(1))
        }
        val block2 = (1..50).map {
            createRandomOwnershipTransferFromEvent()
                .withNewValues(EthereumBlockStatus.CONFIRMED, blockNumber = 2, logIndex = it)
                .copy(value = EthUInt256.of(1))
        }
        val block3 = (1..50).map {
            createRandomOwnershipTransferFromEvent()
                .withNewValues(EthereumBlockStatus.CONFIRMED, blockNumber = 3, logIndex = it)
                .copy(value = EthUInt256.of(1))
        }
        val events = listOf(block1, block2, block3).flatten()

        val reducedOwnership = reduce(ownership, events)
        assertThat(reducedOwnership.value).isEqualTo(EthUInt256.of(1))
        assertThat(reducedOwnership.revertableEvents).hasSizeLessThanOrEqualTo(properties.reduce.maxRevertableEventsAmount)
        assertThat(reducedOwnership.deleted).isFalse()

        val revertedBlock3 = block3.map { event ->
            event.withNewValues(
                EthereumBlockStatus.REVERTED,
                blockNumber = event.log.blockNumber,
                logIndex = event.log.logIndex
            )
        }.reversed()
        val reducedOwnershipWithRevertedBlock3 = reduce(reducedOwnership, revertedBlock3)
        assertThat(reducedOwnershipWithRevertedBlock3.value).isEqualTo(EthUInt256.of(51))
        assertThat(reducedOwnershipWithRevertedBlock3.deleted).isFalse()

        val revertedBlock2 = block2.map { event ->
            event.withNewValues(
                EthereumBlockStatus.REVERTED,
                blockNumber = event.log.blockNumber,
                logIndex = event.log.logIndex
            )
        }.reversed()
        val reducedOwnershipWithRevertedBlock2 = reduce(reducedOwnershipWithRevertedBlock3, revertedBlock2)
        assertThat(reducedOwnershipWithRevertedBlock2.value).isEqualTo(EthUInt256.of(101))
        assertThat(reducedOwnershipWithRevertedBlock2.deleted).isFalse()

        val revertedBlock1 = block1.map { event ->
            event.withNewValues(
                EthereumBlockStatus.REVERTED,
                blockNumber = event.log.blockNumber,
                logIndex = event.log.logIndex
            )
        }.reversed()
        val reducedOwnershipWithRevertedBlock1 = reduce(reducedOwnershipWithRevertedBlock2, revertedBlock1)
        assertThat(reducedOwnershipWithRevertedBlock1.value).isEqualTo(EthUInt256.of(0))
        assertThat(reducedOwnershipWithRevertedBlock1.deleted).isTrue()
    }

    private fun initial(): Ownership {
        val ownershipId = createRandomOwnershipId()
        return ownershipTemplateProvider.getEntityTemplate(ownershipId)
    }

    private suspend fun reduce(ownership: Ownership, events: List<OwnershipEvent>): Ownership {
        return reduce(ownership, *events.toTypedArray())
    }

    private suspend fun reduce(ownership: Ownership, vararg events: OwnershipEvent): Ownership {
        return events.fold(ownership) { entity, event ->
            ownershipReducer.reduce(entity, event)
        }
    }
}
