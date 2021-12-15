package com.rarible.protocol.nft.core.data

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomString
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.BlockchainEntityEvent
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemEvent
import com.rarible.protocol.nft.core.model.OwnershipEvent
import com.rarible.protocol.nft.core.model.Part

fun createRandomItem(): Item {
    return Item.empty(randomAddress(), EthUInt256.of(randomLong()))
}

fun createRandomCreatorsItemEvent(): ItemEvent.ItemCreatorsEvent {
    return ItemEvent.ItemCreatorsEvent(
        creators = listOf(Part.fullPart(randomAddress()), Part.fullPart(randomAddress())),
        blockNumber = randomLong(),
        logIndex = randomInt(),
        status = BlockchainEntityEvent.Status.values().random(),
        entityId = randomString(),
        timestamp = randomLong(),
        transactionHash = randomString(),
        address = randomString(),
        minorLogIndex = randomInt()
    )
}

fun createRandomTransferItemEvent(): ItemEvent.ItemTransferEvent {
    return ItemEvent.ItemTransferEvent(
        from = randomAddress(),
        to = randomAddress(),
        value = EthUInt256.of(randomInt()),
        blockNumber = randomLong(),
        logIndex = randomInt(),
        status = BlockchainEntityEvent.Status.values().random(),
        entityId = randomString(),
        timestamp = randomLong(),
        transactionHash = randomString(),
        address = randomString(),
        minorLogIndex = randomInt()
    )
}

fun createRandomMintItemEvent(): ItemEvent.ItemMintEvent {
    return ItemEvent.ItemMintEvent(
        owner = randomAddress(),
        supply = EthUInt256.of(randomInt()),
        blockNumber = randomLong(),
        logIndex = randomInt(),
        status = BlockchainEntityEvent.Status.values().random(),
        entityId = randomString(),
        timestamp = randomLong(),
        transactionHash = randomString(),
        address = randomString(),
        minorLogIndex = randomInt()
    )
}

fun createRandomBurnItemEvent(): ItemEvent.ItemBurnEvent {
    return ItemEvent.ItemBurnEvent(
        supply = EthUInt256.of(randomInt()),
        owner = randomAddress(),
        blockNumber = randomLong(),
        logIndex = randomInt(),
        status = BlockchainEntityEvent.Status.values().random(),
        entityId = randomString(),
        timestamp = randomLong(),
        transactionHash = randomString(),
        address = randomString(),
        minorLogIndex = randomInt()
    )
}

fun createRandomLazyMintItemEvent(): ItemEvent.LazyItemMintEvent {
    return ItemEvent.LazyItemMintEvent(
        supply = EthUInt256.of(randomInt()),
        creators = emptyList(),
        blockNumber = randomLong(),
        logIndex = randomInt(),
        status = BlockchainEntityEvent.Status.values().random(),
        entityId = randomString(),
        timestamp = randomLong(),
        transactionHash = randomString(),
        address = randomString(),
        minorLogIndex = randomInt()
    )
}
fun createRandomLazyBurnItemEvent(): ItemEvent.LazyItemBurnEvent {
    return ItemEvent.LazyItemBurnEvent(
        supply = EthUInt256.of(randomInt()),
        blockNumber = randomLong(),
        logIndex = randomInt(),
        status = BlockchainEntityEvent.Status.values().random(),
        entityId = randomString(),
        timestamp = randomLong(),
        transactionHash = randomString(),
        address = randomString(),
        minorLogIndex = randomInt()
    )
}

fun createRandomOwnershipTransferToEvent(): OwnershipEvent.TransferToEvent {
    return OwnershipEvent.TransferToEvent(
        from = randomAddress(),
        value = EthUInt256.of(randomInt()),
        blockNumber = randomLong(),
        logIndex = randomInt(),
        status = BlockchainEntityEvent.Status.values().random(),
        entityId = randomString(),
        timestamp = randomLong(),
        transactionHash = randomString(),
        address = randomString(),
        minorLogIndex = randomInt()
    )
}

fun createRandomOwnershipTransferFromEvent(): OwnershipEvent.TransferFromEvent {
    return OwnershipEvent.TransferFromEvent(
        to = randomAddress(),
        value = EthUInt256.of(randomInt()),
        blockNumber = randomLong(),
        logIndex = randomInt(),
        status = BlockchainEntityEvent.Status.values().random(),
        entityId = randomString(),
        timestamp = randomLong(),
        transactionHash = randomString(),
        address = randomString(),
        minorLogIndex = randomInt()
    )
}

fun createRandomOwnershipChangeLazyValueEvent(): OwnershipEvent.ChangeLazyValueEvent {
    return OwnershipEvent.ChangeLazyValueEvent(
        value = EthUInt256.of(randomInt()),
        blockNumber = randomLong(),
        logIndex = randomInt(),
        status = BlockchainEntityEvent.Status.values().random(),
        entityId = randomString(),
        timestamp = randomLong(),
        transactionHash = randomString(),
        address = randomString(),
        minorLogIndex = randomInt()
    )
}
fun createRandomOwnershipLazyTransferToEvent(): OwnershipEvent.LazyTransferToEvent {
    return OwnershipEvent.LazyTransferToEvent(
        value = EthUInt256.of(randomInt()),
        blockNumber = randomLong(),
        logIndex = randomInt(),
        status = BlockchainEntityEvent.Status.values().random(),
        entityId = randomString(),
        timestamp = randomLong(),
        transactionHash = randomString(),
        address = randomString(),
        minorLogIndex = randomInt()
    )
}
