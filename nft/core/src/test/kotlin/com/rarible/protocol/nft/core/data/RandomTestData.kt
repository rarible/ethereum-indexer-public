package com.rarible.protocol.nft.core.data

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomString
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.BlockchainEntityEvent
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemEvent

fun createRandomItem(): Item {
    return Item.empty(randomAddress(), EthUInt256.of(randomLong()))
}

fun createRandomMintItemEvent(): ItemEvent.ItemMintEvent {
    return ItemEvent.ItemMintEvent(
        supply = EthUInt256.of(randomInt()),
        blockNumber = randomLong(),
        logIndex = randomInt(),
        status = BlockchainEntityEvent.Status.values().random(),
        entityId = randomString(),
        timestamp = randomLong(),
        transactionHash = randomString(),
        minorLogIndex = randomInt()
    )
}

fun createRandomBurnItemEvent(): ItemEvent.ItemBurnEvent {
    return ItemEvent.ItemBurnEvent(
        supply = EthUInt256.of(randomInt()),
        blockNumber = randomLong(),
        logIndex = randomInt(),
        status = BlockchainEntityEvent.Status.values().random(),
        entityId = randomString(),
        timestamp = randomLong(),
        transactionHash = randomString(),
        minorLogIndex = randomInt()
    )
}

fun createRandomLazyMintItemEvent(): ItemEvent.LazyItemMintEvent {
    return ItemEvent.LazyItemMintEvent(
        supply = EthUInt256.of(randomInt()),
        blockNumber = randomLong(),
        logIndex = randomInt(),
        status = BlockchainEntityEvent.Status.values().random(),
        entityId = randomString(),
        timestamp = randomLong(),
        transactionHash = randomString(),
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
        minorLogIndex = randomInt()
    )
}
