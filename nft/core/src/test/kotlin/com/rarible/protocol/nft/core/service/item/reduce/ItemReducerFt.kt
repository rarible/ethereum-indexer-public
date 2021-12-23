package com.rarible.protocol.nft.core.service.item.reduce

import com.rarible.blockchain.scanner.framework.model.Log
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
            .withNewValues(Log.Status.CONFIRMED, blockNumber = 1)
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
            .withNewValues(Log.Status.CONFIRMED, blockNumber = 1, logIndex = 1, minorLogIndex = 1)
            .copy(supply = EthUInt256.TEN, owner = minter)
        val revertedMint = mint
            .withNewValues(Log.Status.REVERTED, blockNumber = 1, logIndex = 1, minorLogIndex = 1)

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
            .withNewValues(Log.Status.CONFIRMED, blockNumber = 1)
            .copy(supply = EthUInt256.TEN, owner = minter)
        val burn = createRandomBurnItemEvent()
            .withNewValues(Log.Status.CONFIRMED, blockNumber = 2)
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
            .withNewValues(Log.Status.CONFIRMED, blockNumber = 1, logIndex = 1, minorLogIndex = 1)
            .copy(supply = EthUInt256.TEN, owner = minter)
        val burn = createRandomBurnItemEvent()
            .withNewValues(Log.Status.CONFIRMED, blockNumber = 2, logIndex = 1, minorLogIndex = 1)
            .copy(supply = EthUInt256.TEN, owner = minter)
        val revertedBurn = burn
            .withNewValues(Log.Status.REVERTED, blockNumber = 2, logIndex = 1, minorLogIndex = 1)

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
            .withNewValues(Log.Status.CONFIRMED, blockNumber = 1)
            .copy(supply = EthUInt256.TEN, owner = minter)
        val burn = createRandomBurnItemEvent()
            .withNewValues(Log.Status.CONFIRMED, blockNumber = 2)
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
            .withNewValues(Log.Status.CONFIRMED, blockNumber = 1)
            .copy(supply = EthUInt256.TEN, owner = minter)
        val transfer = createRandomTransferItemEvent()
            .withNewValues(Log.Status.CONFIRMED, blockNumber = 2)
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
            .withNewValues(Log.Status.CONFIRMED, blockNumber = 1, logIndex = 1, minorLogIndex = 1)
            .copy(supply = EthUInt256.TEN, owner = minter)
        val transfer = createRandomTransferItemEvent()
            .withNewValues(Log.Status.CONFIRMED, blockNumber = 2, logIndex = 1, minorLogIndex = 1)
            .copy(value = EthUInt256.TEN, from = minter, to = owner)
        val revertedTransfer = transfer
            .withNewValues(Log.Status.REVERTED, blockNumber = 2, logIndex = 1, minorLogIndex = 1)

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
                .withNewValues(Log.Status.CONFIRMED, blockNumber = 1)
                .copy(supply = EthUInt256.TEN, owner = minter),
            createRandomTransferItemEvent()
                .withNewValues(Log.Status.CONFIRMED, blockNumber = 2)
                .copy(value = EthUInt256.of(2), from = minter, to = owner1),
            createRandomTransferItemEvent()
                .withNewValues(Log.Status.CONFIRMED, blockNumber = 3)
                .copy(value = EthUInt256.of(1), from = owner1, to = owner2),
            createRandomTransferItemEvent()
                .withNewValues(Log.Status.CONFIRMED, blockNumber = 4)
                .copy(value = EthUInt256.of(1), from = owner1, to = owner2),
            createRandomTransferItemEvent()
                .withNewValues(Log.Status.CONFIRMED, blockNumber = 5)
                .copy(value = EthUInt256.of(5), from = minter, to = owner3),
            createRandomTransferItemEvent()
                .withNewValues(Log.Status.CONFIRMED, blockNumber = 6)
                .copy(value = EthUInt256.of(1), from = owner3, to = owner4),
            createRandomTransferItemEvent()
                .withNewValues(Log.Status.CONFIRMED, blockNumber = 7)
                .copy(value = EthUInt256.of(2), from = owner3, to = owner5),
            createRandomTransferItemEvent()
                .withNewValues(Log.Status.CONFIRMED, blockNumber = 8)
                .copy(value = EthUInt256.of(2), from = owner3, to = owner6),
            createRandomTransferItemEvent()
                .withNewValues(Log.Status.CONFIRMED, blockNumber = 9)
                .copy(value = EthUInt256.of(1), from = owner5, to = owner7),
            createRandomBurnItemEvent()
                .withNewValues(Log.Status.CONFIRMED, blockNumber = 10)
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
    fun `should reduce mint and revert multi transfers`() = runBlocking<Unit> {
        val minter = randomAddress()
        val owner1 = randomAddress()
        val owner2 = randomAddress()
        val owner3 = randomAddress()
        val owner4 = randomAddress()
        val owner5 = randomAddress()
        val owner6 = randomAddress()
        val owner7 = randomAddress()
        val item = initial()

        val mint = createRandomMintItemEvent()
            .withNewValues(Log.Status.CONFIRMED, blockNumber = 1, logIndex = 1, minorLogIndex = 1)
            .copy(supply = EthUInt256.TEN, owner = minter)
        val revertedMint = mint
            .withNewValues(Log.Status.REVERTED, blockNumber = 1, logIndex = 1, minorLogIndex = 1)

        val transfer1 = createRandomTransferItemEvent()
            .withNewValues(Log.Status.CONFIRMED, blockNumber = 2, logIndex = 1, minorLogIndex = 1)
            .copy(value = EthUInt256.of(2), from = minter, to = owner1)
        val revertedTransfer1 = transfer1
            .withNewValues(Log.Status.REVERTED, blockNumber = 2, logIndex = 1, minorLogIndex = 1)

        val transfer2 = createRandomTransferItemEvent()
            .withNewValues(Log.Status.CONFIRMED, blockNumber = 3, logIndex = 1, minorLogIndex = 1)
            .copy(value = EthUInt256.of(1), from = owner1, to = owner2)
        val revertedTransfer2 = transfer2
            .withNewValues(Log.Status.REVERTED, blockNumber = 3, logIndex = 1, minorLogIndex = 1)

        val transfer3 = createRandomTransferItemEvent()
            .withNewValues(Log.Status.CONFIRMED, blockNumber = 4, logIndex = 1, minorLogIndex = 1)
            .copy(value = EthUInt256.of(1), from = owner1, to = owner2)
        val revertedTransfer3 = transfer3
            .withNewValues(Log.Status.REVERTED, blockNumber = 4, logIndex = 1, minorLogIndex = 1)

        val transfer4 = createRandomTransferItemEvent()
            .withNewValues(Log.Status.CONFIRMED, blockNumber = 5, logIndex = 1, minorLogIndex = 1)
            .copy(value = EthUInt256.of(5), from = minter, to = owner3)
        val revertedTransfer4 = transfer4
            .withNewValues(Log.Status.REVERTED, blockNumber = 5, logIndex = 1, minorLogIndex = 1)

        val transfer5 = createRandomTransferItemEvent()
            .withNewValues(Log.Status.CONFIRMED, blockNumber = 6, logIndex = 1, minorLogIndex = 1)
            .copy(value = EthUInt256.of(1), from = owner3, to = owner4)
        val revertedTransfer5 = transfer5
            .withNewValues(Log.Status.REVERTED, blockNumber = 6, logIndex = 1, minorLogIndex = 1)

        val transfer6 = createRandomTransferItemEvent()
            .withNewValues(Log.Status.CONFIRMED, blockNumber = 7, logIndex = 1, minorLogIndex = 1)
            .copy(value = EthUInt256.of(2), from = owner3, to = owner5)
        val revertedTransfer6 = transfer6
            .withNewValues(Log.Status.REVERTED, blockNumber = 7, logIndex = 1, minorLogIndex = 1)

        val transfer7 = createRandomTransferItemEvent()
            .withNewValues(Log.Status.CONFIRMED, blockNumber = 8, logIndex = 1, minorLogIndex = 1)
            .copy(value = EthUInt256.of(2), from = owner3, to = owner6)
        val revertedTransfer7 = transfer7
            .withNewValues(Log.Status.REVERTED, blockNumber = 8, logIndex = 1, minorLogIndex = 1)

        val transfer8 = createRandomTransferItemEvent()
            .withNewValues(Log.Status.CONFIRMED, blockNumber = 9, logIndex = 1, minorLogIndex = 1)
            .copy(value = EthUInt256.of(1), from = owner5, to = owner7)
        val revertedTransfer8 = transfer8
            .withNewValues(Log.Status.REVERTED, blockNumber = 9, logIndex = 1, minorLogIndex = 1)

        val burn = createRandomBurnItemEvent()
            .withNewValues(Log.Status.CONFIRMED, blockNumber = 10, logIndex = 1, minorLogIndex = 1)
            .copy(supply = EthUInt256.of(1), owner = owner7)
        val revertedBurn = burn
            .withNewValues(Log.Status.REVERTED, blockNumber = 10, logIndex = 1, minorLogIndex = 1)

        val reducedItem = reduce(
            item,
            mint,
            transfer1,
            transfer2,
            transfer3,
            transfer4,
            transfer5,
            transfer6,
            transfer7,
            transfer8,
            burn
        )
        assertThat(reducedItem.revertableEvents).containsExactlyElementsOf(
            listOf(
                mint, transfer1, transfer2, transfer3, transfer4, transfer5, transfer6, transfer7, transfer8, burn
            )
        )
        assertThat(reducedItem.supply).isEqualTo(EthUInt256.of(9))
        assertThat(reducedItem.ownerships.values.sumOf { it.value.toInt() }).isEqualTo(9)
        assertThat(reducedItem.ownerships.keys).hasSize(5)
        assertThat(reducedItem.ownerships[minter]).isEqualTo(EthUInt256.of(3))
        assertThat(reducedItem.ownerships[owner2]).isEqualTo(EthUInt256.of(2))
        assertThat(reducedItem.ownerships[owner4]).isEqualTo(EthUInt256.of(1))
        assertThat(reducedItem.ownerships[owner5]).isEqualTo(EthUInt256.of(1))
        assertThat(reducedItem.ownerships[owner6]).isEqualTo(EthUInt256.of(2))
        assertThat(reducedItem.deleted).isFalse()

        //Revert burn event
        val revertedBurnItem = reduce(reducedItem, revertedBurn)
        assertThat(revertedBurnItem.revertableEvents).containsExactlyElementsOf(
            listOf(
                mint, transfer1, transfer2, transfer3, transfer4, transfer5, transfer6, transfer7, transfer8
            )
        )
        assertThat(revertedBurnItem.supply).isEqualTo(EthUInt256.TEN)
        assertThat(revertedBurnItem.ownerships.values.sumOf { it.value.toInt() }).isEqualTo(10)
        assertThat(revertedBurnItem.ownerships.keys).hasSize(6)
        assertThat(revertedBurnItem.ownerships[minter]).isEqualTo(EthUInt256.of(3))
        assertThat(revertedBurnItem.ownerships[owner2]).isEqualTo(EthUInt256.of(2))
        assertThat(revertedBurnItem.ownerships[owner4]).isEqualTo(EthUInt256.of(1))
        assertThat(revertedBurnItem.ownerships[owner5]).isEqualTo(EthUInt256.of(1))
        assertThat(revertedBurnItem.ownerships[owner6]).isEqualTo(EthUInt256.of(2))
        assertThat(revertedBurnItem.ownerships[owner7]).isEqualTo(EthUInt256.of(1))
        assertThat(revertedBurnItem.deleted).isFalse()

        //Revert transfer8 event
        val revertedTransfer8Item = reduce(revertedBurnItem, revertedTransfer8)
        assertThat(revertedTransfer8Item.revertableEvents).containsExactlyElementsOf(
            listOf(
                mint, transfer1, transfer2, transfer3, transfer4, transfer5, transfer6, transfer7
            )
        )
        assertThat(revertedTransfer8Item.supply).isEqualTo(EthUInt256.TEN)
        assertThat(revertedTransfer8Item.ownerships.values.sumOf { it.value.toInt() }).isEqualTo(10)
        assertThat(revertedTransfer8Item.ownerships.keys).hasSize(5)
        assertThat(revertedTransfer8Item.ownerships[minter]).isEqualTo(EthUInt256.of(3))
        assertThat(revertedTransfer8Item.ownerships[owner2]).isEqualTo(EthUInt256.of(2))
        assertThat(revertedTransfer8Item.ownerships[owner4]).isEqualTo(EthUInt256.of(1))
        assertThat(revertedTransfer8Item.ownerships[owner5]).isEqualTo(EthUInt256.of(2))
        assertThat(revertedTransfer8Item.ownerships[owner6]).isEqualTo(EthUInt256.of(2))
        assertThat(revertedTransfer8Item.deleted).isFalse()

        //Revert transfer7 event
        val revertedTransfer7Item = reduce(revertedTransfer8Item, revertedTransfer7)
        assertThat(revertedTransfer7Item.revertableEvents).containsExactlyElementsOf(
            listOf(
                mint, transfer1, transfer2, transfer3, transfer4, transfer5, transfer6
            )
        )
        assertThat(revertedTransfer7Item.supply).isEqualTo(EthUInt256.TEN)
        assertThat(revertedTransfer7Item.ownerships.values.sumOf { it.value.toInt() }).isEqualTo(10)
        assertThat(revertedTransfer7Item.ownerships.keys).hasSize(5)
        assertThat(revertedTransfer7Item.ownerships[minter]).isEqualTo(EthUInt256.of(3))
        assertThat(revertedTransfer7Item.ownerships[owner2]).isEqualTo(EthUInt256.of(2))
        assertThat(revertedTransfer7Item.ownerships[owner3]).isEqualTo(EthUInt256.of(2))
        assertThat(revertedTransfer7Item.ownerships[owner4]).isEqualTo(EthUInt256.of(1))
        assertThat(revertedTransfer7Item.ownerships[owner5]).isEqualTo(EthUInt256.of(2))
        assertThat(revertedTransfer7Item.deleted).isFalse()

        //Revert transfer6 event
        val revertedTransfer6Item = reduce(revertedTransfer7Item, revertedTransfer6)
        assertThat(revertedTransfer6Item.revertableEvents).containsExactlyElementsOf(
            listOf(
                mint, transfer1, transfer2, transfer3, transfer4, transfer5
            )
        )
        assertThat(revertedTransfer6Item.supply).isEqualTo(EthUInt256.TEN)
        assertThat(revertedTransfer6Item.ownerships.values.sumOf { it.value.toInt() }).isEqualTo(10)
        assertThat(revertedTransfer6Item.ownerships.keys).hasSize(4)
        assertThat(revertedTransfer6Item.ownerships[minter]).isEqualTo(EthUInt256.of(3))
        assertThat(revertedTransfer6Item.ownerships[owner2]).isEqualTo(EthUInt256.of(2))
        assertThat(revertedTransfer6Item.ownerships[owner3]).isEqualTo(EthUInt256.of(4))
        assertThat(revertedTransfer6Item.ownerships[owner4]).isEqualTo(EthUInt256.of(1))
        assertThat(revertedTransfer6Item.deleted).isFalse()

        //Revert transfer5 event
        val revertedTransfer5Item = reduce(revertedTransfer6Item, revertedTransfer5)
        assertThat(revertedTransfer5Item.revertableEvents).containsExactlyElementsOf(
            listOf(
                mint, transfer1, transfer2, transfer3, transfer4
            )
        )
        assertThat(revertedTransfer5Item.supply).isEqualTo(EthUInt256.TEN)
        assertThat(revertedTransfer5Item.ownerships.values.sumOf { it.value.toInt() }).isEqualTo(10)
        assertThat(revertedTransfer5Item.ownerships.keys).hasSize(3)
        assertThat(revertedTransfer5Item.ownerships[minter]).isEqualTo(EthUInt256.of(3))
        assertThat(revertedTransfer5Item.ownerships[owner2]).isEqualTo(EthUInt256.of(2))
        assertThat(revertedTransfer5Item.ownerships[owner3]).isEqualTo(EthUInt256.of(5))
        assertThat(revertedTransfer5Item.deleted).isFalse()

        //Revert transfer4 event
        val revertedTransfer4Item = reduce(revertedTransfer5Item, revertedTransfer4)
        assertThat(revertedTransfer4Item.revertableEvents).containsExactlyElementsOf(
            listOf(
                mint, transfer1, transfer2, transfer3
            )
        )
        assertThat(revertedTransfer4Item.supply).isEqualTo(EthUInt256.TEN)
        assertThat(revertedTransfer4Item.ownerships.values.sumOf { it.value.toInt() }).isEqualTo(10)
        assertThat(revertedTransfer4Item.ownerships.keys).hasSize(2)
        assertThat(revertedTransfer4Item.ownerships[minter]).isEqualTo(EthUInt256.of(8))
        assertThat(revertedTransfer4Item.ownerships[owner2]).isEqualTo(EthUInt256.of(2))
        assertThat(revertedTransfer4Item.deleted).isFalse()

        //Revert transfer3 event
        val revertedTransfer3Item = reduce(revertedTransfer4Item, revertedTransfer3)
        assertThat(revertedTransfer3Item.revertableEvents).containsExactlyElementsOf(
            listOf(
                mint, transfer1, transfer2
            )
        )
        assertThat(revertedTransfer3Item.supply).isEqualTo(EthUInt256.TEN)
        assertThat(revertedTransfer3Item.ownerships.values.sumOf { it.value.toInt() }).isEqualTo(10)
        assertThat(revertedTransfer3Item.ownerships.keys).hasSize(3)
        assertThat(revertedTransfer3Item.ownerships[minter]).isEqualTo(EthUInt256.of(8))
        assertThat(revertedTransfer3Item.ownerships[owner1]).isEqualTo(EthUInt256.of(1))
        assertThat(revertedTransfer3Item.ownerships[owner2]).isEqualTo(EthUInt256.of(1))
        assertThat(revertedTransfer3Item.deleted).isFalse()

        //Revert transfer2 event
        val revertedTransfer2Item = reduce(revertedTransfer3Item, revertedTransfer2)
        assertThat(revertedTransfer2Item.revertableEvents).containsExactlyElementsOf(
            listOf(
                mint, transfer1
            )
        )
        assertThat(revertedTransfer2Item.supply).isEqualTo(EthUInt256.TEN)
        assertThat(revertedTransfer2Item.ownerships.values.sumOf { it.value.toInt() }).isEqualTo(10)
        assertThat(revertedTransfer2Item.ownerships.keys).hasSize(2)
        assertThat(revertedTransfer2Item.ownerships[minter]).isEqualTo(EthUInt256.of(8))
        assertThat(revertedTransfer2Item.ownerships[owner1]).isEqualTo(EthUInt256.of(2))
        assertThat(revertedTransfer2Item.deleted).isFalse()

        //Revert transfer1 event
        val revertedTransfer1Item = reduce(revertedTransfer2Item, revertedTransfer1)
        assertThat(revertedTransfer1Item.revertableEvents).containsExactlyElementsOf(
            listOf(
                mint
            )
        )
        assertThat(revertedTransfer1Item.supply).isEqualTo(EthUInt256.TEN)
        assertThat(revertedTransfer1Item.ownerships.values.sumOf { it.value.toInt() }).isEqualTo(10)
        assertThat(revertedTransfer1Item.ownerships.keys).hasSize(1)
        assertThat(revertedTransfer1Item.ownerships[minter]).isEqualTo(EthUInt256.of(10))
        assertThat(revertedTransfer1Item.deleted).isFalse()

        //Revert mint event
        val revertedMintItem = reduce(revertedTransfer1Item, revertedMint)
        assertThat(revertedMintItem.revertableEvents).hasSize(0)
        assertThat(revertedMintItem.supply).isEqualTo(EthUInt256.ZERO)
        assertThat(revertedMintItem.ownerships.keys).hasSize(0)
        assertThat(revertedMintItem.deleted).isTrue()
    }

    @Test
    fun `should has only needed confirm events in the revertableEvents`() = runBlocking<Unit> {
        val item = initial()

        val mint = createRandomMintItemEvent()
            .withNewValues(Log.Status.CONFIRMED, blockNumber = 1)
        val transfer1 = createRandomTransferItemEvent()
            .withNewValues(Log.Status.CONFIRMED, blockNumber = 2)
        val transfer2 = createRandomTransferItemEvent()
            .withNewValues(Log.Status.CONFIRMED, blockNumber = 3)
        val transfer3 = createRandomTransferItemEvent()
            .withNewValues(Log.Status.CONFIRMED, blockNumber = 14)
        val transfer4 = createRandomTransferItemEvent()
            .withNewValues(Log.Status.CONFIRMED, blockNumber = 15)
        val transfer5 = createRandomTransferItemEvent()
            .withNewValues(Log.Status.CONFIRMED, blockNumber = 16)

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
            .withNewValues(Log.Status.CONFIRMED, blockNumber = 1, logIndex = 1, minorLogIndex = 1)
            .copy(supply = EthUInt256.TEN, owner = minter)
        val duplicate1 = event1.withNewValues(blockNumber = 1, logIndex = 1, minorLogIndex = 1)
        val event2 = createRandomTransferItemEvent()
            .withNewValues(Log.Status.CONFIRMED, blockNumber = 2)
            .copy(value = EthUInt256.of(2), from = minter, to = owner1)
        val event3 = createRandomTransferItemEvent()
            .withNewValues(Log.Status.CONFIRMED, blockNumber = 3, logIndex = 1, minorLogIndex = 1)
            .copy(value = EthUInt256.of(1), from = owner1, to = owner2)
        val duplicate2 = event3.withNewValues(blockNumber = 3, logIndex = 1, minorLogIndex = 1)
        val event4 = createRandomTransferItemEvent()
            .withNewValues(Log.Status.CONFIRMED, blockNumber = 4)
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
