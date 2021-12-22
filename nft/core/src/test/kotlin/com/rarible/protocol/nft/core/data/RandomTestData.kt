package com.rarible.protocol.nft.core.data

import com.rarible.blockchain.scanner.ethereum.model.EthereumLog
import com.rarible.blockchain.scanner.framework.model.Log
import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomString
import com.rarible.core.test.data.randomWord
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemEvent
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.OwnershipEvent
import com.rarible.protocol.nft.core.model.OwnershipId
import com.rarible.protocol.nft.core.model.Part
import com.rarible.protocol.nft.core.repository.data.createAddress
import com.rarible.protocol.nft.core.repository.data.createItemHistory
import io.daonomic.rpc.domain.Word
import scalether.domain.AddressFactory
import java.math.BigInteger
import java.time.Instant
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.min

fun createRandomItemId(): ItemId {
    return ItemId(randomAddress(), EthUInt256.of(randomBigInt()))
}

fun createRandomItem(): Item {
    return Item.empty(randomAddress(), EthUInt256.of(randomLong()))
}

fun createRandomEthereumLog(): EthereumLog =
    EthereumLog(
        transactionHash = randomWord(),
        status = Log.Status.values().random(),
        address = randomAddress(),
        topic = Word.apply(randomWord()),
        blockHash = Word.apply(randomWord()),
        blockNumber = randomLong(),
        logIndex = randomInt(),
        minorLogIndex = randomInt(),
        index = randomInt(),
        createdAt = nowMillis(),
        updatedAt = nowMillis()
    )

fun EthereumLog.withNewValues(
    status: Log.Status? = null,
    createdAt: Instant? = null,
    blockNumber: Long? = null,
    logIndex: Int? = null,
    minorLogIndex: Int? = null
) = copy(
    status = status ?: this.status,
    createdAt = createdAt ?: this.createdAt,
    blockNumber = blockNumber ?: if (this.blockNumber != null && blockNumber == null) null else this.blockNumber,
    logIndex = logIndex ?: if (this.logIndex != null && logIndex == null) null else this.logIndex,
    minorLogIndex = minorLogIndex ?: this.minorLogIndex
)

fun ItemEvent.ItemMintEvent.withNewValues(
    status: Log.Status? = null,
    createdAt: Instant? = null,
    blockNumber: Long? = null,
    logIndex: Int? = null,
    minorLogIndex: Int? = null
) = copy(log = log.withNewValues(status, createdAt, blockNumber, logIndex, minorLogIndex))

fun ItemEvent.ItemBurnEvent.withNewValues(
    status: Log.Status? = null,
    createdAt: Instant? = null,
    blockNumber: Long? = null,
    logIndex: Int? = null,
    minorLogIndex: Int? = null
) = copy(log = log.withNewValues(status, createdAt, blockNumber, logIndex, minorLogIndex))

fun ItemEvent.ItemTransferEvent.withNewValues(
    status: Log.Status? = null,
    createdAt: Instant? = null,
    blockNumber: Long? = null,
    logIndex: Int? = null,
    minorLogIndex: Int? = null
) = copy(log = log.withNewValues(status, createdAt, blockNumber, logIndex, minorLogIndex))

fun ItemEvent.ItemCreatorsEvent.withNewValues(
    status: Log.Status? = null,
    createdAt: Instant? = null
) = copy(log = log.withNewValues(status, createdAt))

fun ItemEvent.LazyItemMintEvent.withNewValues(
    status: Log.Status? = null,
    createdAt: Instant? = null
) = copy(log = log.withNewValues(status, createdAt))

fun ItemEvent.LazyItemBurnEvent.withNewValues(
    status: Log.Status? = null,
    createdAt: Instant? = null
) = copy(log = log.withNewValues(status, createdAt))

fun createRandomCreatorsItemEvent(): ItemEvent.ItemCreatorsEvent {
    return ItemEvent.ItemCreatorsEvent(
        creators = listOf(Part.fullPart(randomAddress()), Part.fullPart(randomAddress())),
        entityId = randomString(),
        log = createRandomEthereumLog()
    )
}

fun createRandomTransferItemEvent(): ItemEvent.ItemTransferEvent {
    return ItemEvent.ItemTransferEvent(
        from = randomAddress(),
        to = randomAddress(),
        value = EthUInt256.of(randomInt()),
        entityId = randomString(),
        log = createRandomEthereumLog()
    )
}

fun createRandomMintItemEvent(): ItemEvent.ItemMintEvent {
    return ItemEvent.ItemMintEvent(
        owner = randomAddress(),
        supply = EthUInt256.of(randomInt()),
        entityId = randomString(),
        log = createRandomEthereumLog()
    )
}

fun createRandomBurnItemEvent(): ItemEvent.ItemBurnEvent {
    return ItemEvent.ItemBurnEvent(
        supply = EthUInt256.of(randomInt()),
        owner = randomAddress(),
        entityId = randomString(),
        log = createRandomEthereumLog()
    )
}

fun createRandomLazyMintItemEvent(): ItemEvent.LazyItemMintEvent {
    return ItemEvent.LazyItemMintEvent(
        supply = EthUInt256.of(randomInt()),
        creators = emptyList(),
        entityId = randomString(),
        log = createRandomEthereumLog()
    )
}
fun createRandomLazyBurnItemEvent(): ItemEvent.LazyItemBurnEvent {
    return ItemEvent.LazyItemBurnEvent(
        supply = EthUInt256.of(randomInt()),
        entityId = randomString(),
        log = createRandomEthereumLog()
    )
}

fun createRandomOwnershipTransferToEvent(): OwnershipEvent.TransferToEvent {
    return OwnershipEvent.TransferToEvent(
        from = randomAddress(),
        value = EthUInt256.of(randomInt()),
        entityId = randomString(),
        log = createRandomEthereumLog()
    )
}

fun createRandomOwnershipId(): OwnershipId {
    return OwnershipId(randomAddress(), EthUInt256.of(randomBigInt()), randomAddress())
}

fun createRandomOwnership(): Ownership {
    val token = createAddress()
    val owner = createAddress()
    val tokenId = EthUInt256.of(ThreadLocalRandom.current().nextLong(1, 10000))
    return Ownership(
        token = token,
        tokenId = tokenId,
        creators = listOf(Part(AddressFactory.create(), 1000)),
        owner = owner,
        value = EthUInt256.of(BigInteger.valueOf(ThreadLocalRandom.current().nextLong(1, 10000))),
        lazyValue = EthUInt256.of(BigInteger.valueOf(ThreadLocalRandom.current().nextLong(1, 10000))),
        date = nowMillis(),
        pending = (1..ThreadLocalRandom.current().nextInt(1, 20)).map { createItemHistory() }
    )
}

fun createRandomOwnershipTransferFromEvent(): OwnershipEvent.TransferFromEvent {
    return OwnershipEvent.TransferFromEvent(
        to = randomAddress(),
        value = EthUInt256.of(randomInt()),
        entityId = randomString(),
        log = createRandomEthereumLog()
    )
}

fun createRandomOwnershipChangeLazyValueEvent(): OwnershipEvent.ChangeLazyValueEvent {
    return OwnershipEvent.ChangeLazyValueEvent(
        value = EthUInt256.of(randomInt()),
        entityId = randomString(),
        log = createRandomEthereumLog()
    )
}
fun createRandomOwnershipLazyTransferToEvent(): OwnershipEvent.LazyTransferToEvent {
    return OwnershipEvent.LazyTransferToEvent(
        value = EthUInt256.of(randomInt()),
        entityId = randomString(),
        log = createRandomEthereumLog()
    )
}

fun OwnershipEvent.LazyTransferToEvent.withNewValues(
    status: Log.Status? = null,
    createdAt: Instant? = null,
    blockNumber: Long? = null
) = copy(log = log.withNewValues(status, createdAt, blockNumber))

fun OwnershipEvent.TransferFromEvent.withNewValues(
    status: Log.Status? = null,
    createdAt: Instant? = null,
    blockNumber: Long? = null,
    logIndex: Int? = null,
    minorLogIndex: Int? = null
) = copy(log = log.withNewValues(status, createdAt, blockNumber, logIndex, minorLogIndex))

fun OwnershipEvent.TransferToEvent.withNewValues(
    status: Log.Status? = null,
    createdAt: Instant? = null,
    blockNumber: Long? = null,
    logIndex: Int? = null,
    minorLogIndex: Int? = null
) = copy(log = log.withNewValues(status, createdAt, blockNumber, logIndex, minorLogIndex))
