package com.rarible.protocol.nft.core.service.item.reduce

import com.rarible.blockchain.scanner.ethereum.model.EthereumLogStatus
import com.rarible.core.test.data.randomAddress
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.data.createRandomBurnItemEvent
import com.rarible.protocol.nft.core.data.createRandomItemId
import com.rarible.protocol.nft.core.data.createRandomMintItemEvent
import com.rarible.protocol.nft.core.data.createRandomTransferItemEvent
import com.rarible.protocol.nft.core.data.withNewValues
import com.rarible.protocol.nft.core.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.core.integration.IntegrationTest
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemEvent
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
internal class ItemReducerFt : AbstractIntegrationTest() {
    @Autowired
    private lateinit var itemReducer: ItemReducer

    @Autowired
    private lateinit var itemTemplateProvider: ItemTemplateProvider

    @Test
    fun `should reduce simple mint event`() = runBlocking<Unit> {
        val minter = randomAddress()
        val item = initial()
        val mint = createRandomMintItemEvent()
            .withNewValues(EthereumLogStatus.CONFIRMED, blockNumber = 1)
            .copy(supply = EthUInt256.TEN, owner = minter)

        val reducedItem = reduce(item, mint)
        assertThat(reducedItem.supply).isEqualTo(EthUInt256.TEN)
        assertThat(reducedItem.lazySupply).isEqualTo(EthUInt256.ZERO)
        assertThat(reducedItem.ownerships.keys).hasSize(1)
        assertThat(reducedItem.ownerships[minter]).isEqualTo(EthUInt256.TEN)
        assertThat(reducedItem.deleted).isFalse()
    }

    @Test
    fun `should reduce revert simple mint event`() = runBlocking<Unit> {
        val minter = randomAddress()
        val item = initial()

        val mint = createRandomMintItemEvent()
            .withNewValues(EthereumLogStatus.CONFIRMED, blockNumber = 1, logIndex = 1, minorLogIndex = 1)
            .copy(supply = EthUInt256.TEN, owner = minter)
        val revertedMint = mint
            .withNewValues(EthereumLogStatus.REVERTED, blockNumber = 1, logIndex = 1, minorLogIndex = 1)

        val reducedItem = reduce(item, mint, revertedMint)
        assertThat(reducedItem.supply).isEqualTo(EthUInt256.ZERO)
        assertThat(reducedItem.lazySupply).isEqualTo(EthUInt256.ZERO)
        assertThat(reducedItem.ownerships.keys).hasSize(0)
        assertThat(reducedItem.deleted).isTrue()
    }

    @Test
    fun `should reduce simple mint and full burn event`() = runBlocking<Unit> {
        val minter = randomAddress()
        val item = initial()

        val mint = createRandomMintItemEvent()
            .withNewValues(EthereumLogStatus.CONFIRMED, blockNumber = 1)
            .copy(supply = EthUInt256.TEN, owner = minter)
        val burn = createRandomBurnItemEvent()
            .withNewValues(EthereumLogStatus.CONFIRMED, blockNumber = 2)
            .copy(supply = EthUInt256.TEN, owner = minter)

        val reducedItem = reduce(item, mint, burn)
        assertThat(reducedItem.supply).isEqualTo(EthUInt256.ZERO)
        assertThat(reducedItem.lazySupply).isEqualTo(EthUInt256.ZERO)
        assertThat(reducedItem.ownerships.keys).hasSize(0)
        assertThat(reducedItem.deleted).isTrue()
    }

    @Test
    fun `should reduce simple mint and revert full burn event`() = runBlocking<Unit> {
        val minter = randomAddress()
        val item = initial()

        val mint = createRandomMintItemEvent()
            .withNewValues(EthereumLogStatus.CONFIRMED, blockNumber = 1, logIndex = 1, minorLogIndex = 1)
            .copy(supply = EthUInt256.TEN, owner = minter)
        val burn = createRandomBurnItemEvent()
            .withNewValues(EthereumLogStatus.CONFIRMED, blockNumber = 2, logIndex = 1, minorLogIndex = 1)
            .copy(supply = EthUInt256.TEN, owner = minter)
        val revertedBurn = burn
            .withNewValues(EthereumLogStatus.REVERTED, blockNumber = 2, logIndex = 1, minorLogIndex = 1)

        val burnItem = reduce(item, mint, burn)
        assertThat(burnItem.deleted).isTrue()

        val revertedItem = reduce(burnItem, revertedBurn)
        assertThat(revertedItem.supply).isEqualTo(EthUInt256.TEN)
        assertThat(revertedItem.lazySupply).isEqualTo(EthUInt256.ZERO)
        assertThat(revertedItem.ownerships.keys).hasSize(1)
        assertThat(revertedItem.ownerships[minter]).isEqualTo(EthUInt256.TEN)
        assertThat(revertedItem.deleted).isFalse()
    }

    @Test
    fun `should reduce simple mint and burn event`() = runBlocking<Unit> {
        val minter = randomAddress()
        val item = initial()

        val mint = createRandomMintItemEvent()
            .withNewValues(EthereumLogStatus.CONFIRMED, blockNumber = 1)
            .copy(supply = EthUInt256.TEN, owner = minter)
        val burn = createRandomBurnItemEvent()
            .withNewValues(EthereumLogStatus.CONFIRMED, blockNumber = 2)
            .copy(supply = EthUInt256.ONE, owner = minter)

        val reducedItem = reduce(item, mint, burn)
        assertThat(reducedItem.supply).isEqualTo(EthUInt256.of(9))
        assertThat(reducedItem.lazySupply).isEqualTo(EthUInt256.ZERO)
        assertThat(reducedItem.ownerships.keys).hasSize(1)
        assertThat(reducedItem.ownerships[minter]).isEqualTo(EthUInt256.of(9))
        assertThat(reducedItem.deleted).isFalse()
    }

    @Test
    fun `should reduce mint and one full transfer`() = runBlocking<Unit> {
        val minter = randomAddress()
        val owner = randomAddress()
        val item = initial()

        val mint = createRandomMintItemEvent()
            .withNewValues(EthereumLogStatus.CONFIRMED, blockNumber = 1)
            .copy(supply = EthUInt256.TEN, owner = minter)
        val transfer = createRandomTransferItemEvent()
            .withNewValues(EthereumLogStatus.CONFIRMED, blockNumber = 2)
            .copy(value = EthUInt256.TEN, from = minter, to = owner)

        val reducedItem = reduce(item, mint, transfer)
        assertThat(reducedItem.supply).isEqualTo(EthUInt256.TEN)
        assertThat(reducedItem.lazySupply).isEqualTo(EthUInt256.ZERO)
        assertThat(reducedItem.ownerships.keys).hasSize(1)
        assertThat(reducedItem.ownerships[owner]).isEqualTo(EthUInt256.TEN)
        assertThat(reducedItem.deleted).isFalse()
    }

    @Test
    fun `should reduce mint and one revert full transfer`() = runBlocking<Unit> {
        val minter = randomAddress()
        val owner = randomAddress()
        val item = initial()

        val mint = createRandomMintItemEvent()
            .withNewValues(EthereumLogStatus.CONFIRMED, blockNumber = 1, logIndex = 1, minorLogIndex = 1)
            .copy(supply = EthUInt256.TEN, owner = minter)
        val transfer = createRandomTransferItemEvent()
            .withNewValues(EthereumLogStatus.CONFIRMED, blockNumber = 2, logIndex = 1, minorLogIndex = 1)
            .copy(value = EthUInt256.TEN, from = minter, to = owner)
        val revertedTransfer = transfer
            .withNewValues(EthereumLogStatus.REVERTED, blockNumber = 2, logIndex = 1, minorLogIndex = 1)

        val reducedItem = reduce(item, mint, transfer)

        val revertedItem = reduce(reducedItem, revertedTransfer)
        assertThat(revertedItem.supply).isEqualTo(EthUInt256.TEN)
        assertThat(revertedItem.lazySupply).isEqualTo(EthUInt256.ZERO)
        assertThat(revertedItem.ownerships.keys).hasSize(1)
        assertThat(revertedItem.ownerships[minter]).isEqualTo(EthUInt256.TEN)
        assertThat(revertedItem.deleted).isFalse()
    }

    @Test
    fun `should reduce mint and multi transfers`() = runBlocking<Unit> {
        val minter = randomAddress()
        val owner1 = randomAddress()
        val owner2 = randomAddress()
        val owner3 = randomAddress()
        val owner4 = randomAddress()
        val owner5 = randomAddress()
        val owner6 = randomAddress()
        val owner7 = randomAddress()
        val item = initial()

        val events = listOf(
            createRandomMintItemEvent()
                .withNewValues(EthereumLogStatus.CONFIRMED, blockNumber = 1)
                .copy(supply = EthUInt256.TEN, owner = minter),
            createRandomTransferItemEvent()
                .withNewValues(EthereumLogStatus.CONFIRMED, blockNumber = 2)
                .copy(value = EthUInt256.of(2), from = minter, to = owner1),
            createRandomTransferItemEvent()
                .withNewValues(EthereumLogStatus.CONFIRMED, blockNumber = 3)
                .copy(value = EthUInt256.of(1), from = owner1, to = owner2),
            createRandomTransferItemEvent()
                .withNewValues(EthereumLogStatus.CONFIRMED, blockNumber = 4)
                .copy(value = EthUInt256.of(1), from = owner1, to = owner2),
            createRandomTransferItemEvent()
                .withNewValues(EthereumLogStatus.CONFIRMED, blockNumber = 5)
                .copy(value = EthUInt256.of(5), from = minter, to = owner3),
            createRandomTransferItemEvent()
                .withNewValues(EthereumLogStatus.CONFIRMED, blockNumber = 6)
                .copy(value = EthUInt256.of(1), from = owner3, to = owner4),
            createRandomTransferItemEvent()
                .withNewValues(EthereumLogStatus.CONFIRMED, blockNumber = 7)
                .copy(value = EthUInt256.of(2), from = owner3, to = owner5),
            createRandomTransferItemEvent()
                .withNewValues(EthereumLogStatus.CONFIRMED, blockNumber = 8)
                .copy(value = EthUInt256.of(2), from = owner3, to = owner6),
            createRandomTransferItemEvent()
                .withNewValues(EthereumLogStatus.CONFIRMED, blockNumber = 9)
                .copy(value = EthUInt256.of(1), from = owner5, to = owner7),
            createRandomBurnItemEvent()
                .withNewValues(EthereumLogStatus.CONFIRMED, blockNumber = 10)
                .copy(supply = EthUInt256.of(1), owner = owner7),
        )

        val reducedItem = reduce(item, events)
        assertThat(reducedItem.supply).isEqualTo(EthUInt256.of(9))
        assertThat(reducedItem.ownerships.values.sumOf { it.value.toInt() }).isEqualTo(9)
        assertThat(reducedItem.lazySupply).isEqualTo(EthUInt256.ZERO)
        assertThat(reducedItem.ownerships.keys).hasSize(5)
        assertThat(reducedItem.ownerships[minter]).isEqualTo(EthUInt256.of(3))
        assertThat(reducedItem.ownerships[owner2]).isEqualTo(EthUInt256.of(2))
        assertThat(reducedItem.ownerships[owner4]).isEqualTo(EthUInt256.of(1))
        assertThat(reducedItem.ownerships[owner5]).isEqualTo(EthUInt256.of(1))
        assertThat(reducedItem.ownerships[owner6]).isEqualTo(EthUInt256.of(2))
        assertThat(reducedItem.revertableEvents).containsExactlyElementsOf(events)
        assertThat(reducedItem.deleted).isFalse()
    }

    @Test
    fun `should has only needed confirm events in the revertableEvents`() = runBlocking<Unit> {
        val item = initial()

        val mint = createRandomMintItemEvent()
            .withNewValues(EthereumLogStatus.CONFIRMED, blockNumber = 1)
        val transfer1 = createRandomTransferItemEvent()
            .withNewValues(EthereumLogStatus.CONFIRMED, blockNumber = 2)
        val transfer2 = createRandomTransferItemEvent()
            .withNewValues(EthereumLogStatus.CONFIRMED, blockNumber = 3)
        val transfer3 = createRandomTransferItemEvent()
            .withNewValues(EthereumLogStatus.CONFIRMED, blockNumber = 14)
        val transfer4 = createRandomTransferItemEvent()
            .withNewValues(EthereumLogStatus.CONFIRMED, blockNumber = 15)
        val transfer5 = createRandomTransferItemEvent()
            .withNewValues(EthereumLogStatus.CONFIRMED, blockNumber = 16)

        val reducedItem = reduce(item, mint, transfer1, transfer2, transfer3, transfer4, transfer5)

        assertThat(reducedItem.revertableEvents).containsExactlyElementsOf(
            listOf(transfer2, transfer3, transfer4, transfer5)
        )
    }

    @Test
    fun `should not handle applied events`() = runBlocking<Unit> {
        val minter = randomAddress()
        val owner1 = randomAddress()
        val owner2 = randomAddress()
        val item = initial()

        val event1 = createRandomMintItemEvent()
            .withNewValues(EthereumLogStatus.CONFIRMED, blockNumber = 1, logIndex = 1, minorLogIndex = 1)
            .copy(supply = EthUInt256.TEN, owner = minter)
        val duplicate1 = event1.withNewValues(blockNumber = 1, logIndex = 1, minorLogIndex = 1)
        val event2 = createRandomTransferItemEvent()
            .withNewValues(EthereumLogStatus.CONFIRMED, blockNumber = 2)
            .copy(value = EthUInt256.of(2), from = minter, to = owner1)
        val event3 = createRandomTransferItemEvent()
            .withNewValues(EthereumLogStatus.CONFIRMED, blockNumber = 3, logIndex = 1, minorLogIndex = 1)
            .copy(value = EthUInt256.of(1), from = owner1, to = owner2)
        val duplicate2 = event3.withNewValues(blockNumber = 3, logIndex = 1, minorLogIndex = 1)
        val event4 = createRandomTransferItemEvent()
            .withNewValues(EthereumLogStatus.CONFIRMED, blockNumber = 4)
            .copy(value = EthUInt256.of(1), from = owner1, to = owner2)

        val reducedItem = reduce(
            item, event1, duplicate1, event2, event3, duplicate2, duplicate1, event4)
        assertThat(reducedItem.supply).isEqualTo(EthUInt256.TEN)
        assertThat(reducedItem.ownerships.values.sumOf { it.value.toInt() }).isEqualTo(10)
        assertThat(reducedItem.lazySupply).isEqualTo(EthUInt256.ZERO)
        assertThat(reducedItem.ownerships.keys).hasSize(2)
        assertThat(reducedItem.ownerships[minter]).isEqualTo(EthUInt256.of(8))
        assertThat(reducedItem.ownerships[owner2]).isEqualTo(EthUInt256.of(2))
        assertThat(reducedItem.revertableEvents).containsExactlyElementsOf(listOf(
            event1, event2, event3, event4
        ))
        assertThat(reducedItem.deleted).isFalse()
    }

    private fun initial(): Item {
        val itemId = createRandomItemId()
        return itemTemplateProvider.getEntityTemplate(itemId)
    }

    private suspend fun reduce(item: Item, events: List<ItemEvent>): Item {
        return reduce(item, *events.toTypedArray())
    }

    private suspend fun reduce(item: Item, vararg events: ItemEvent): Item {
        return events.fold(item) { entity, event ->
            itemReducer.reduce(entity, event)
        }
    }
}
