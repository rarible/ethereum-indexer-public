package com.rarible.protocol.nft.core.service.item.reduce

import com.rarible.blockchain.scanner.ethereum.model.EthereumBlockStatus
import com.rarible.core.test.data.randomAddress
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.data.createRandomBurnItemEvent
import com.rarible.protocol.nft.core.data.createRandomCreatorsItemEvent
import com.rarible.protocol.nft.core.data.createRandomItem
import com.rarible.protocol.nft.core.data.createRandomItemId
import com.rarible.protocol.nft.core.data.createRandomLazyMintItemEvent
import com.rarible.protocol.nft.core.data.createRandomMintItemEvent
import com.rarible.protocol.nft.core.data.createRandomOpenSeaLazyItemMintEvent
import com.rarible.protocol.nft.core.data.createRandomTransferItemEvent
import com.rarible.protocol.nft.core.data.withNewValues
import com.rarible.protocol.nft.core.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.core.integration.IntegrationTest
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemEvent
import com.rarible.protocol.nft.core.model.Part
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import scalether.domain.Address
import java.math.BigInteger

@IntegrationTest
internal class ItemReducerFt : AbstractIntegrationTest() {
    @Autowired
    private lateinit var itemReducer: ItemReducer

    @Autowired
    private lateinit var properties: NftIndexerProperties

    @Autowired
    private lateinit var itemTemplateProvider: ItemTemplateProvider

    @Test
    fun `should reduce simple mint event`() = runBlocking<Unit> {
        val minter = randomAddress()
        val item = initial()
        val mint = createRandomMintItemEvent()
            .withNewValues(EthereumBlockStatus.CONFIRMED, blockNumber = 1)
            .copy(supply = EthUInt256.TEN, owner = minter)

        val reducedItem = reduce(item, mint)
        assertThat(reducedItem.supply).isEqualTo(EthUInt256.TEN)
        assertThat(reducedItem.lazySupply).isEqualTo(EthUInt256.ZERO)
        assertThat(reducedItem.deleted).isFalse()
    }

    @Test
    fun `should reduce mint event and revert event with exist item`() = runBlocking<Unit> {
        val item = initial().copy(
            supply = EthUInt256.ONE,
            revertableEvents = emptyList()
        )
        val mint = createRandomMintItemEvent()
            .withNewValues(EthereumBlockStatus.CONFIRMED, blockNumber = 1, logIndex = 1, minorLogIndex = 1)
        val revertedMint = mint
            .withNewValues(EthereumBlockStatus.REVERTED, blockNumber = 1, logIndex = 1, minorLogIndex = 1)

        val reducedItem = reduce(item, mint, revertedMint)
        assertThat(reducedItem.supply).isEqualTo(EthUInt256.ONE)
        assertThat(reducedItem.deleted).isFalse
        assertThat(reducedItem.revertableEvents).isEmpty()
    }

    @Test
    fun `should reduce burn event and revert event with exist item`() = runBlocking<Unit> {
        val item = initial().copy(
            supply = EthUInt256.ONE,
            revertableEvents = emptyList()
        )
        val burn = createRandomBurnItemEvent() .copy(supply = EthUInt256.ONE)
            .withNewValues(EthereumBlockStatus.CONFIRMED, blockNumber = 1, logIndex = 1, minorLogIndex = 1)
        val revertedBurn = burn
            .withNewValues(EthereumBlockStatus.REVERTED, blockNumber = 1, logIndex = 1, minorLogIndex = 1)

        val reducedItem = reduce(item, burn, revertedBurn)
        assertThat(reducedItem.supply).isEqualTo(EthUInt256.ONE)
        assertThat(reducedItem.deleted).isFalse
        assertThat(reducedItem.revertableEvents).isEmpty()
    }

    @Test
    @Disabled // TODO PT-798 doesn't applicable with V2 reducer/scanner
    fun `should reduce simple mint event with pending and revert it`() = runBlocking<Unit> {
        val minter = randomAddress()
        val item = initial()
        val mint = createRandomMintItemEvent()
            .withNewValues(EthereumBlockStatus.PENDING, blockNumber = null, minorLogIndex = 0)
            .copy(supply = EthUInt256.TEN, owner = minter)

        val reducedItem1 = reduce(item, mint)
        assertThat(reducedItem1.revertableEvents).hasSize(1)
        assertThat(reducedItem1.date).isEqualTo(mint.log.createdAt)

        val inactiveMint = mint.withNewValues(EthereumBlockStatus.INACTIVE)
        val reducedItem2 = reduce(reducedItem1, inactiveMint)
        assertThat(reducedItem2.revertableEvents).hasSize(0)
    }

    @Test
    fun `should reduce revert simple mint event`() = runBlocking<Unit> {
        val minter = randomAddress()
        val item = initial()

        val mint = createRandomMintItemEvent()
            .withNewValues(EthereumBlockStatus.CONFIRMED, blockNumber = 1, logIndex = 1, minorLogIndex = 1)
            .copy(supply = EthUInt256.TEN, owner = minter)
        val revertedMint = mint
            .withNewValues(EthereumBlockStatus.REVERTED, blockNumber = 1, logIndex = 1, minorLogIndex = 1)

        val reducedItem = reduce(item, mint, revertedMint)
        assertThat(reducedItem.supply).isEqualTo(EthUInt256.ZERO)
        assertThat(reducedItem.lazySupply).isEqualTo(EthUInt256.ZERO)
        assertThat(reducedItem.deleted).isTrue()
    }

    @Test
    fun `should reduce simple mint and full burn event`() = runBlocking<Unit> {
        val minter = randomAddress()
        val item = initial()

        val mint = createRandomMintItemEvent()
            .withNewValues(EthereumBlockStatus.CONFIRMED, blockNumber = 1)
            .copy(supply = EthUInt256.TEN, owner = minter)
        val burn = createRandomBurnItemEvent()
            .withNewValues(EthereumBlockStatus.CONFIRMED, blockNumber = 2)
            .copy(supply = EthUInt256.TEN, owner = minter)

        val reducedItem = reduce(item, mint, burn)
        assertThat(reducedItem.supply).isEqualTo(EthUInt256.ZERO)
        assertThat(reducedItem.lazySupply).isEqualTo(EthUInt256.ZERO)
        assertThat(reducedItem.deleted).isTrue()
        assertThat(reducedItem.date).isEqualTo(burn.log.createdAt)
    }

    @Test
    fun `should reduce simple mint and revert full burn event`() = runBlocking<Unit> {
        val minter = randomAddress()
        val item = initial()

        val mint = createRandomMintItemEvent()
            .withNewValues(EthereumBlockStatus.CONFIRMED, blockNumber = 1, logIndex = 1, minorLogIndex = 1)
            .copy(supply = EthUInt256.TEN, owner = minter)
        val burn = createRandomBurnItemEvent()
            .withNewValues(EthereumBlockStatus.CONFIRMED, blockNumber = 2, logIndex = 1, minorLogIndex = 1)
            .copy(supply = EthUInt256.TEN, owner = minter)
        val revertedBurn = burn
            .withNewValues(EthereumBlockStatus.REVERTED, blockNumber = 2, logIndex = 1, minorLogIndex = 1)

        val burnItem = reduce(item, mint, burn)
        assertThat(burnItem.deleted).isTrue()

        val revertedItem = reduce(burnItem, revertedBurn)
        assertThat(revertedItem.supply).isEqualTo(EthUInt256.TEN)
        assertThat(revertedItem.lazySupply).isEqualTo(EthUInt256.ZERO)
        assertThat(revertedItem.deleted).isFalse()
        assertThat(revertedItem.date).isEqualTo(mint.log.createdAt)
    }

    @Test
    fun `should reduce simple mint and burn event`() = runBlocking<Unit> {
        val minter = randomAddress()
        val item = initial()

        val mint = createRandomMintItemEvent()
            .withNewValues(EthereumBlockStatus.CONFIRMED, blockNumber = 1)
            .copy(supply = EthUInt256.TEN, owner = minter)
        val burn = createRandomBurnItemEvent()
            .withNewValues(EthereumBlockStatus.CONFIRMED, blockNumber = 2)
            .copy(supply = EthUInt256.ONE, owner = minter)

        val reducedItem = reduce(item, mint, burn)
        assertThat(reducedItem.supply).isEqualTo(EthUInt256.of(9))
        assertThat(reducedItem.lazySupply).isEqualTo(EthUInt256.ZERO)
        assertThat(reducedItem.deleted).isFalse()
    }

    @Test
    fun `should reduce mint and one full transfer`() = runBlocking<Unit> {
        val minter = randomAddress()
        val owner = randomAddress()
        val item = initial()

        val mint = createRandomMintItemEvent()
            .withNewValues(EthereumBlockStatus.CONFIRMED, blockNumber = 1)
            .copy(supply = EthUInt256.TEN, owner = minter)
        val transfer = createRandomTransferItemEvent()
            .withNewValues(EthereumBlockStatus.CONFIRMED, blockNumber = 2)
            .copy(value = EthUInt256.TEN, from = minter, to = owner)

        val reducedItem = reduce(item, mint, transfer)
        assertThat(reducedItem.supply).isEqualTo(EthUInt256.TEN)
        assertThat(reducedItem.lazySupply).isEqualTo(EthUInt256.ZERO)
        assertThat(reducedItem.deleted).isFalse()
    }

    @Test
    fun `should reduce mint and one revert full transfer`() = runBlocking<Unit> {
        val minter = randomAddress()
        val owner = randomAddress()
        val item = initial()

        val mint = createRandomMintItemEvent()
            .withNewValues(EthereumBlockStatus.CONFIRMED, blockNumber = 1, logIndex = 1, minorLogIndex = 1)
            .copy(supply = EthUInt256.TEN, owner = minter)
        val transfer = createRandomTransferItemEvent()
            .withNewValues(EthereumBlockStatus.CONFIRMED, blockNumber = 2, logIndex = 1, minorLogIndex = 1)
            .copy(value = EthUInt256.TEN, from = minter, to = owner)
        val revertedTransfer = transfer
            .withNewValues(EthereumBlockStatus.REVERTED, blockNumber = 2, logIndex = 1, minorLogIndex = 1)

        val reducedItem = reduce(item, mint, transfer)

        val revertedItem = reduce(reducedItem, revertedTransfer)
        assertThat(revertedItem.supply).isEqualTo(EthUInt256.TEN)
        assertThat(revertedItem.lazySupply).isEqualTo(EthUInt256.ZERO)
        assertThat(revertedItem.deleted).isFalse()
    }

    @Test
    fun `should reduce lazy mint, mint, transfer and creator`() = runBlocking<Unit> {
        val minter = randomAddress()
        val creators = listOf(Part.fullPart(minter))
        val owner = randomAddress()
        val item = initial()

        val lazyMint = createRandomLazyMintItemEvent()
            .copy(supply = EthUInt256.ONE, creators = creators)
        val mint = createRandomMintItemEvent()
            .withNewValues(EthereumBlockStatus.CONFIRMED, blockNumber = 1, logIndex = 1)
            .copy(supply = EthUInt256.ONE, owner = minter)
        val transfer = createRandomTransferItemEvent()
            .withNewValues(EthereumBlockStatus.CONFIRMED, blockNumber = 1, logIndex = 2)
            .copy(value = EthUInt256.ONE, from = minter, to = owner)
        val creator = createRandomCreatorsItemEvent()
            .withNewValues(EthereumBlockStatus.CONFIRMED, blockNumber = 1, logIndex = 3)
            .copy(creators = creators)

        val reducedItem = reduce(item, lazyMint, mint, transfer, creator)
        assertThat(reducedItem.supply).isEqualTo(EthUInt256.ONE)
        assertThat(reducedItem.lazySupply).isEqualTo(EthUInt256.ZERO)
        assertThat(reducedItem.creators).isEqualTo(creators)
        assertThat(reducedItem.deleted).isFalse()
        assertThat(reducedItem.date).isEqualTo(creator.log.createdAt)
    }

    @Test
    fun `should reduce lazy mint, mint, transfer and creator and revert them all`() = runBlocking<Unit> {
        val minter = randomAddress()
        val creators = listOf(Part.fullPart(minter))
        val owner = randomAddress()
        val item = initial()

        val lazyMint = createRandomLazyMintItemEvent()
            .copy(supply = EthUInt256.ONE, creators = creators)

        val mint = createRandomMintItemEvent()
            .withNewValues(EthereumBlockStatus.CONFIRMED, blockNumber = 1, logIndex = 1, minorLogIndex = 1)
            .copy(supply = EthUInt256.ONE, owner = minter)
        val revertedMint = mint
            .withNewValues(EthereumBlockStatus.REVERTED, blockNumber = 1, logIndex = 1, minorLogIndex = 1)

        val transfer = createRandomTransferItemEvent()
            .withNewValues(EthereumBlockStatus.CONFIRMED, blockNumber = 1, logIndex = 2, minorLogIndex = 2)
            .copy(value = EthUInt256.ONE, from = minter, to = owner)
        val revertedTransfer = transfer
            .withNewValues(EthereumBlockStatus.REVERTED, blockNumber = 1, logIndex = 2, minorLogIndex = 2)

        val creator = createRandomCreatorsItemEvent()
            .withNewValues(EthereumBlockStatus.CONFIRMED, blockNumber = 1, logIndex = 3, minorLogIndex = 1)
            .copy(creators = creators)
        val revertedCreator = creator
            .withNewValues(EthereumBlockStatus.REVERTED, blockNumber = 1, logIndex = 3, minorLogIndex = 1)

        val reducedItem = reduce(item, lazyMint, mint, transfer, creator, revertedCreator, revertedTransfer, revertedMint)

        assertThat(reducedItem.supply).isEqualTo(EthUInt256.ONE)
        assertThat(reducedItem.lazySupply).isEqualTo(EthUInt256.ONE)
        assertThat(reducedItem.creators).isEqualTo(creators)
        assertThat(reducedItem.deleted).isFalse()
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
                .withNewValues(EthereumBlockStatus.CONFIRMED, blockNumber = 1)
                .copy(supply = EthUInt256.TEN, owner = minter),
            createRandomTransferItemEvent()
                .withNewValues(EthereumBlockStatus.CONFIRMED, blockNumber = 2)
                .copy(value = EthUInt256.of(2), from = minter, to = owner1),
            createRandomTransferItemEvent()
                .withNewValues(EthereumBlockStatus.CONFIRMED, blockNumber = 3)
                .copy(value = EthUInt256.of(1), from = owner1, to = owner2),
            createRandomTransferItemEvent()
                .withNewValues(EthereumBlockStatus.CONFIRMED, blockNumber = 4)
                .copy(value = EthUInt256.of(1), from = owner1, to = owner2),
            createRandomTransferItemEvent()
                .withNewValues(EthereumBlockStatus.CONFIRMED, blockNumber = 5)
                .copy(value = EthUInt256.of(5), from = minter, to = owner3),
            createRandomTransferItemEvent()
                .withNewValues(EthereumBlockStatus.CONFIRMED, blockNumber = 6)
                .copy(value = EthUInt256.of(1), from = owner3, to = owner4),
            createRandomTransferItemEvent()
                .withNewValues(EthereumBlockStatus.CONFIRMED, blockNumber = 7)
                .copy(value = EthUInt256.of(2), from = owner3, to = owner5),
            createRandomTransferItemEvent()
                .withNewValues(EthereumBlockStatus.CONFIRMED, blockNumber = 8)
                .copy(value = EthUInt256.of(2), from = owner3, to = owner6),
            createRandomTransferItemEvent()
                .withNewValues(EthereumBlockStatus.CONFIRMED, blockNumber = 9)
                .copy(value = EthUInt256.of(1), from = owner5, to = owner7),
            createRandomBurnItemEvent()
                .withNewValues(EthereumBlockStatus.CONFIRMED, blockNumber = 10)
                .copy(supply = EthUInt256.of(1), owner = owner7),
        )

        val reducedItem = reduce(item, events)
        assertThat(reducedItem.supply).isEqualTo(EthUInt256.of(9))
        assertThat(reducedItem.lazySupply).isEqualTo(EthUInt256.ZERO)
        assertThat(reducedItem.revertableEvents).containsExactlyElementsOf(events)
        assertThat(reducedItem.deleted).isFalse()
    }

    @Test
    fun `revert compacted events`() = runBlocking<Unit> {
        val item = createRandomItem().copy(
            supply = EthUInt256.ZERO,
            lazySupply = EthUInt256.ZERO,
            deleted = true,
            revertableEvents = emptyList()
        )
        val block1 = (1..101).map {
            createRandomMintItemEvent()
                .withNewValues(EthereumBlockStatus.CONFIRMED, blockNumber = 1, logIndex = it)
                .copy(supply = EthUInt256.of(1))
        }
        val block2 = (1..50).map {
            createRandomBurnItemEvent()
                .withNewValues(EthereumBlockStatus.CONFIRMED, blockNumber = 2, logIndex = it)
                .copy(supply = EthUInt256.of(1))
        }
        val block3 = (1..50).map {
            createRandomBurnItemEvent()
                .withNewValues(EthereumBlockStatus.CONFIRMED, blockNumber = 3, logIndex = it)
                .copy(supply = EthUInt256.of(1))
        }
        val events = listOf(block1, block2, block3).flatten()

        val reducedItem = reduce(item, events)
        assertThat(reducedItem.supply).isEqualTo(EthUInt256.of(1))
        assertThat(reducedItem.revertableEvents).hasSizeLessThanOrEqualTo(properties.reduce.maxRevertableEventsAmount)
        assertThat(reducedItem.deleted).isFalse()

        val revertedBlock3 = block3.map { event ->
            event.withNewValues(
                EthereumBlockStatus.REVERTED,
                blockNumber = event.log.blockNumber,
                logIndex = event.log.logIndex
            )
        }.reversed()
        val reducedItemWithRevertedBlock3 = reduce(reducedItem, revertedBlock3)
        assertThat(reducedItemWithRevertedBlock3.supply).isEqualTo(EthUInt256.of(51))
        assertThat(reducedItemWithRevertedBlock3.deleted).isFalse()

        val revertedBlock2 = block2.map { event ->
            event.withNewValues(
                EthereumBlockStatus.REVERTED,
                blockNumber = event.log.blockNumber,
                logIndex = event.log.logIndex
            )
        }.reversed()
        val reducedItemWithRevertedBlock2 = reduce(reducedItemWithRevertedBlock3, revertedBlock2)
        assertThat(reducedItemWithRevertedBlock2.supply).isEqualTo(EthUInt256.of(101))
        assertThat(reducedItemWithRevertedBlock2.deleted).isFalse()

        val revertedBlock1 = block1.map { event ->
            event.withNewValues(
                EthereumBlockStatus.REVERTED,
                blockNumber = event.log.blockNumber,
                logIndex = event.log.logIndex
            )
        }.reversed()
        val reducedItemWithRevertedBlock1 = reduce(reducedItemWithRevertedBlock2, revertedBlock1)
        assertThat(reducedItemWithRevertedBlock1.supply).isEqualTo(EthUInt256.of(0))
        assertThat(reducedItemWithRevertedBlock1.deleted).isTrue()
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
            .withNewValues(EthereumBlockStatus.CONFIRMED, blockNumber = 1, logIndex = 1, minorLogIndex = 1)
            .copy(supply = EthUInt256.TEN, owner = minter)
        val revertedMint = mint
            .withNewValues(EthereumBlockStatus.REVERTED, blockNumber = 1, logIndex = 1, minorLogIndex = 1)

        val transfer1 = createRandomTransferItemEvent()
            .withNewValues(EthereumBlockStatus.CONFIRMED, blockNumber = 2, logIndex = 1, minorLogIndex = 1)
            .copy(value = EthUInt256.of(2), from = minter, to = owner1)
        val revertedTransfer1 = transfer1
            .withNewValues(EthereumBlockStatus.REVERTED, blockNumber = 2, logIndex = 1, minorLogIndex = 1)

        val transfer2 = createRandomTransferItemEvent()
            .withNewValues(EthereumBlockStatus.CONFIRMED, blockNumber = 3, logIndex = 1, minorLogIndex = 1)
            .copy(value = EthUInt256.of(1), from = owner1, to = owner2)
        val revertedTransfer2 = transfer2
            .withNewValues(EthereumBlockStatus.REVERTED, blockNumber = 3, logIndex = 1, minorLogIndex = 1)

        val transfer3 = createRandomTransferItemEvent()
            .withNewValues(EthereumBlockStatus.CONFIRMED, blockNumber = 4, logIndex = 1, minorLogIndex = 1)
            .copy(value = EthUInt256.of(1), from = owner1, to = owner2)
        val revertedTransfer3 = transfer3
            .withNewValues(EthereumBlockStatus.REVERTED, blockNumber = 4, logIndex = 1, minorLogIndex = 1)

        val transfer4 = createRandomTransferItemEvent()
            .withNewValues(EthereumBlockStatus.CONFIRMED, blockNumber = 5, logIndex = 1, minorLogIndex = 1)
            .copy(value = EthUInt256.of(5), from = minter, to = owner3)
        val revertedTransfer4 = transfer4
            .withNewValues(EthereumBlockStatus.REVERTED, blockNumber = 5, logIndex = 1, minorLogIndex = 1)

        val transfer5 = createRandomTransferItemEvent()
            .withNewValues(EthereumBlockStatus.CONFIRMED, blockNumber = 6, logIndex = 1, minorLogIndex = 1)
            .copy(value = EthUInt256.of(1), from = owner3, to = owner4)
        val revertedTransfer5 = transfer5
            .withNewValues(EthereumBlockStatus.REVERTED, blockNumber = 6, logIndex = 1, minorLogIndex = 1)

        val transfer6 = createRandomTransferItemEvent()
            .withNewValues(EthereumBlockStatus.CONFIRMED, blockNumber = 7, logIndex = 1, minorLogIndex = 1)
            .copy(value = EthUInt256.of(2), from = owner3, to = owner5)
        val revertedTransfer6 = transfer6
            .withNewValues(EthereumBlockStatus.REVERTED, blockNumber = 7, logIndex = 1, minorLogIndex = 1)

        val transfer7 = createRandomTransferItemEvent()
            .withNewValues(EthereumBlockStatus.CONFIRMED, blockNumber = 8, logIndex = 1, minorLogIndex = 1)
            .copy(value = EthUInt256.of(2), from = owner3, to = owner6)
        val revertedTransfer7 = transfer7
            .withNewValues(EthereumBlockStatus.REVERTED, blockNumber = 8, logIndex = 1, minorLogIndex = 1)

        val transfer8 = createRandomTransferItemEvent()
            .withNewValues(EthereumBlockStatus.CONFIRMED, blockNumber = 9, logIndex = 1, minorLogIndex = 1)
            .copy(value = EthUInt256.of(1), from = owner5, to = owner7)
        val revertedTransfer8 = transfer8
            .withNewValues(EthereumBlockStatus.REVERTED, blockNumber = 9, logIndex = 1, minorLogIndex = 1)

        val burn = createRandomBurnItemEvent()
            .withNewValues(EthereumBlockStatus.CONFIRMED, blockNumber = 10, logIndex = 1, minorLogIndex = 1)
            .copy(supply = EthUInt256.of(1), owner = owner7)
        val revertedBurn = burn
            .withNewValues(EthereumBlockStatus.REVERTED, blockNumber = 10, logIndex = 1, minorLogIndex = 1)

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
        assertThat(reducedItem.deleted).isFalse()

        //Revert burn event
        val revertedBurnItem = reduce(reducedItem, revertedBurn)
        assertThat(revertedBurnItem.revertableEvents).containsExactlyElementsOf(
            listOf(
                mint, transfer1, transfer2, transfer3, transfer4, transfer5, transfer6, transfer7, transfer8
            )
        )
        assertThat(revertedBurnItem.supply).isEqualTo(EthUInt256.TEN)
        assertThat(revertedBurnItem.deleted).isFalse()

        //Revert transfer8 event
        val revertedTransfer8Item = reduce(revertedBurnItem, revertedTransfer8)
        assertThat(revertedTransfer8Item.revertableEvents).containsExactlyElementsOf(
            listOf(
                mint, transfer1, transfer2, transfer3, transfer4, transfer5, transfer6, transfer7
            )
        )
        assertThat(revertedTransfer8Item.supply).isEqualTo(EthUInt256.TEN)
        assertThat(revertedTransfer8Item.deleted).isFalse()

        //Revert transfer7 event
        val revertedTransfer7Item = reduce(revertedTransfer8Item, revertedTransfer7)
        assertThat(revertedTransfer7Item.revertableEvents).containsExactlyElementsOf(
            listOf(
                mint, transfer1, transfer2, transfer3, transfer4, transfer5, transfer6
            )
        )
        assertThat(revertedTransfer7Item.supply).isEqualTo(EthUInt256.TEN)
        assertThat(revertedTransfer7Item.deleted).isFalse()

        //Revert transfer6 event
        val revertedTransfer6Item = reduce(revertedTransfer7Item, revertedTransfer6)
        assertThat(revertedTransfer6Item.revertableEvents).containsExactlyElementsOf(
            listOf(
                mint, transfer1, transfer2, transfer3, transfer4, transfer5
            )
        )
        assertThat(revertedTransfer6Item.supply).isEqualTo(EthUInt256.TEN)
        assertThat(revertedTransfer6Item.deleted).isFalse()

        //Revert transfer5 event
        val revertedTransfer5Item = reduce(revertedTransfer6Item, revertedTransfer5)
        assertThat(revertedTransfer5Item.revertableEvents).containsExactlyElementsOf(
            listOf(
                mint, transfer1, transfer2, transfer3, transfer4
            )
        )
        assertThat(revertedTransfer5Item.supply).isEqualTo(EthUInt256.TEN)
        assertThat(revertedTransfer5Item.deleted).isFalse()

        //Revert transfer4 event
        val revertedTransfer4Item = reduce(revertedTransfer5Item, revertedTransfer4)
        assertThat(revertedTransfer4Item.revertableEvents).containsExactlyElementsOf(
            listOf(
                mint, transfer1, transfer2, transfer3
            )
        )
        assertThat(revertedTransfer4Item.supply).isEqualTo(EthUInt256.TEN)
        assertThat(revertedTransfer4Item.deleted).isFalse()

        //Revert transfer3 event
        val revertedTransfer3Item = reduce(revertedTransfer4Item, revertedTransfer3)
        assertThat(revertedTransfer3Item.revertableEvents).containsExactlyElementsOf(
            listOf(
                mint, transfer1, transfer2
            )
        )
        assertThat(revertedTransfer3Item.supply).isEqualTo(EthUInt256.TEN)
        assertThat(revertedTransfer3Item.deleted).isFalse()

        //Revert transfer2 event
        val revertedTransfer2Item = reduce(revertedTransfer3Item, revertedTransfer2)
        assertThat(revertedTransfer2Item.revertableEvents).containsExactlyElementsOf(
            listOf(
                mint, transfer1
            )
        )
        assertThat(revertedTransfer2Item.supply).isEqualTo(EthUInt256.TEN)
        assertThat(revertedTransfer2Item.deleted).isFalse()

        //Revert transfer1 event
        val revertedTransfer1Item = reduce(revertedTransfer2Item, revertedTransfer1)
        assertThat(revertedTransfer1Item.revertableEvents).containsExactlyElementsOf(
            listOf(
                mint
            )
        )
        assertThat(revertedTransfer1Item.supply).isEqualTo(EthUInt256.TEN)
        assertThat(revertedTransfer1Item.deleted).isFalse()

        //Revert mint event
        val revertedMintItem = reduce(revertedTransfer1Item, revertedMint)
        assertThat(revertedMintItem.revertableEvents).hasSize(0)
        assertThat(revertedMintItem.supply).isEqualTo(EthUInt256.ZERO)
        assertThat(revertedMintItem.deleted).isTrue()
    }

    @Test
    fun `should has only needed confirm events in the revertableEvents`() = runBlocking<Unit> {
        val item = initial()

        val mint = createRandomMintItemEvent()
            .withNewValues(EthereumBlockStatus.CONFIRMED, blockNumber = 1)
        val transfer1 = createRandomTransferItemEvent()
            .withNewValues(EthereumBlockStatus.CONFIRMED, blockNumber = 2)
        val transfer2 = createRandomTransferItemEvent()
            .withNewValues(EthereumBlockStatus.CONFIRMED, blockNumber = 3)
        val transfer3 = createRandomTransferItemEvent()
            .withNewValues(EthereumBlockStatus.CONFIRMED, blockNumber = 14)
        val transfer4 = createRandomTransferItemEvent()
            .withNewValues(EthereumBlockStatus.CONFIRMED, blockNumber = 15)
        val transfer5 = createRandomTransferItemEvent()
            .withNewValues(EthereumBlockStatus.CONFIRMED, blockNumber = 16)

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
            .withNewValues(EthereumBlockStatus.CONFIRMED, blockNumber = 1, logIndex = 1, minorLogIndex = 1)
            .copy(supply = EthUInt256.TEN, owner = minter)
        val duplicate1 = event1.withNewValues(blockNumber = 1, logIndex = 1, minorLogIndex = 1)
        val event2 = createRandomTransferItemEvent()
            .withNewValues(EthereumBlockStatus.CONFIRMED, blockNumber = 2)
            .copy(value = EthUInt256.of(2), from = minter, to = owner1)
        val event3 = createRandomTransferItemEvent()
            .withNewValues(EthereumBlockStatus.CONFIRMED, blockNumber = 3, logIndex = 1, minorLogIndex = 1)
            .copy(value = EthUInt256.of(1), from = owner1, to = owner2)
        val duplicate2 = event3.withNewValues(blockNumber = 3, logIndex = 1, minorLogIndex = 1)
        val event4 = createRandomTransferItemEvent()
            .withNewValues(EthereumBlockStatus.CONFIRMED, blockNumber = 4)
            .copy(value = EthUInt256.of(1), from = owner1, to = owner2)

        val reducedItem = reduce(
            item, event1, duplicate1, event2, event3, duplicate2, duplicate1, event4)
        assertThat(reducedItem.supply).isEqualTo(EthUInt256.TEN)
        assertThat(reducedItem.lazySupply).isEqualTo(EthUInt256.ZERO)
        assertThat(reducedItem.revertableEvents).containsExactlyElementsOf(listOf(
            event1, event2, event3, event4
        ))
        assertThat(reducedItem.deleted).isFalse()
    }

    @Test
    fun `should mint and transfer opensea lazy item`() = runBlocking<Unit> {
        val minter = Address.apply("0x47921676A46CcFe3D80b161c7B4DDC8Ed9e716B6")
        val tokenId = EthUInt256.of(BigInteger("32372326957878872325869669322028881416287194712918919938492792330334129619037"))

        val item = initial().copy(
            tokenId = tokenId,
            supply = EthUInt256.ONE
        )
        val event = createRandomOpenSeaLazyItemMintEvent()
            .withNewValues(
                EthereumBlockStatus.CONFIRMED,
                blockNumber = 1,
                logIndex = 1,
                minorLogIndex = 1,
                address = Address.apply(properties.openseaLazyMintAddress)
            )
            .copy(
                supply = EthUInt256.TEN,
                from = minter,
            )

        val reducedItem = reduce(item, event)
        assertThat(reducedItem.supply).isEqualTo(EthUInt256.of(11))
        assertThat(reducedItem.lazySupply).isEqualTo(EthUInt256.ZERO)
        assertThat(reducedItem.creators).hasSize(1)
        assertThat(reducedItem.creators[0].account).isEqualTo(minter)
        assertThat(reducedItem.deleted).isFalse()
    }

    @Test
    fun `should mint and transfer and then revert opensea lazy item`() = runBlocking<Unit> {
        val minter = Address.apply("0x47921676A46CcFe3D80b161c7B4DDC8Ed9e716B6")
        val owner = randomAddress()
        val tokenId = EthUInt256.of(BigInteger("32372326957878872325869669322028881416287194712918919938492792330334129619037"))
        val item = initial().copy(
            tokenId = tokenId,
            supply = EthUInt256.ONE
        )
        val event = createRandomTransferItemEvent()
            .withNewValues(
                EthereumBlockStatus.CONFIRMED,
                blockNumber = 1,
                logIndex = 1,
                minorLogIndex = 1,
                address = Address.apply(properties.openseaLazyMintAddress)
            ).copy(
                value = EthUInt256.TEN,
                from = minter,
                to = owner
            )
        val revertedEvent = event
            .withNewValues(EthereumBlockStatus.REVERTED, blockNumber = 1, logIndex = 1, minorLogIndex = 1)

        val reducedItem = reduce(item, event, revertedEvent)
        assertThat(reducedItem.supply).isEqualTo(EthUInt256.ONE)
        assertThat(reducedItem.lazySupply).isEqualTo(EthUInt256.ZERO)
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
