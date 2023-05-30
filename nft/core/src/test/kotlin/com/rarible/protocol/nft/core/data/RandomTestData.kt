package com.rarible.protocol.nft.core.data

import com.rarible.blockchain.scanner.ethereum.model.EthereumLog
import com.rarible.blockchain.scanner.ethereum.model.EthereumLogStatus
import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomBoolean
import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomString
import com.rarible.core.test.data.randomWord
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.ActionState
import com.rarible.protocol.nft.core.model.BurnItemAction
import com.rarible.protocol.nft.core.model.BurnItemActionEvent
import com.rarible.protocol.nft.core.model.EventData
import com.rarible.protocol.nft.core.model.InconsistentItem
import com.rarible.protocol.nft.core.model.InconsistentItemStatus
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemEvent
import com.rarible.protocol.nft.core.model.ItemExState
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProblemType
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.model.ItemTransfer
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.OwnershipEvent
import com.rarible.protocol.nft.core.model.OwnershipId
import com.rarible.protocol.nft.core.model.Part
import com.rarible.protocol.nft.core.model.TokenProperties
import com.rarible.protocol.nft.core.model.UpdateSuspiciousItemsState
import com.rarible.protocol.nft.core.model.indexerInNftBlockchainTimeMark
import com.rarible.protocol.nft.core.repository.data.createAddress
import com.rarible.protocol.nft.core.repository.data.createItemHistory
import com.rarible.protocol.nft.core.service.item.meta.properties.ContentBuilder
import io.daonomic.rpc.domain.Word
import org.apache.commons.lang3.RandomUtils
import scalether.domain.Address
import scalether.domain.AddressFactory
import java.math.BigInteger
import java.time.Instant
import java.util.concurrent.ThreadLocalRandom

fun createRandomItemId(): ItemId {
    return ItemId(randomAddress(), EthUInt256.of(randomBigInt()))
}

fun createRandomItem(): Item {
    return Item.empty(randomAddress(), EthUInt256.of(randomLong()))
}

fun createRandomUrl(): String =
    "https://image.com/${randomString()}"

fun createRandomEthereumLog(
    transactionSender: Address = randomAddress()
): EthereumLog =
    EthereumLog(
        transactionHash = randomWord(),
        status = EthereumLogStatus.values().random(),
        address = randomAddress(),
        topic = Word.apply(randomWord()),
        blockHash = Word.apply(randomWord()),
        blockNumber = randomLong(),
        logIndex = randomInt(),
        minorLogIndex = randomInt(),
        index = randomInt(),
        from = transactionSender,
        blockTimestamp = nowMillis().epochSecond,
        createdAt = nowMillis()
    )

fun createRandomReversedEthereumLogRecord(data: EventData): ReversedEthereumLogRecord =
    ReversedEthereumLogRecord(
        id = randomString(),
        version = randomLong(),
        data = data,
        log = createRandomEthereumLog()
    )

fun createRandomItemTransfer(): ItemTransfer =
    ItemTransfer(
        owner = randomAddress(),
        token = randomAddress(),
        tokenId = EthUInt256(randomBigInt()),
        date = nowMillis(),
        from = randomAddress(),
        value = EthUInt256(randomBigInt())
    )

fun EthereumLog.withNewValues(
    status: EthereumLogStatus? = null,
    createdAt: Instant? = null,
    blockNumber: Long? = null,
    logIndex: Int? = null,
    minorLogIndex: Int? = null,
    address: Address? = null,
    from: Address? = null,
    index: Int? = null
) = copy(
    status = status ?: this.status,
    createdAt = createdAt ?: this.createdAt,
    blockNumber = blockNumber ?: if (this.blockNumber != null) null else this.blockNumber,
    logIndex = logIndex ?: if (this.logIndex != null) null else this.logIndex,
    index = index ?: this.index,
    minorLogIndex = minorLogIndex ?: this.minorLogIndex,
    address = address ?: this.address,
    from = from ?: this.from
)

fun ItemEvent.ItemMintEvent.withNewValues(
    status: EthereumLogStatus? = null,
    createdAt: Instant? = null,
    blockNumber: Long? = null,
    logIndex: Int? = null,
    minorLogIndex: Int? = null,
    address: Address? = null,
    index: Int? = null,
) = copy(log = log.withNewValues(status, createdAt, blockNumber, logIndex, minorLogIndex, index = index))

fun ItemEvent.ItemBurnEvent.withNewValues(
    status: EthereumLogStatus? = null,
    createdAt: Instant? = null,
    blockNumber: Long? = null,
    logIndex: Int? = null,
    minorLogIndex: Int? = null
) = copy(log = log.withNewValues(status, createdAt, blockNumber, logIndex, minorLogIndex))

fun ItemEvent.ItemTransferEvent.withNewValues(
    status: EthereumLogStatus? = null,
    createdAt: Instant? = null,
    blockNumber: Long? = null,
    logIndex: Int? = null,
    minorLogIndex: Int? = null,
    from: Address? = null,
    address: Address? = null
) = copy(
    log = log.withNewValues(
        status,
        createdAt,
        blockNumber,
        logIndex,
        minorLogIndex,
        address = address,
        from = from
    )
)

fun ItemEvent.OpenSeaLazyItemMintEvent.withNewValues(
    status: EthereumLogStatus? = null,
    createdAt: Instant? = null,
    blockNumber: Long? = null,
    logIndex: Int? = null,
    minorLogIndex: Int? = null,
    from: Address? = null,
    address: Address? = null
) = copy(
    log = log.withNewValues(
        status,
        createdAt,
        blockNumber,
        logIndex,
        minorLogIndex,
        address = address,
        from = from
    )
)

fun ItemEvent.ItemCreatorsEvent.withNewValues(
    status: EthereumLogStatus? = null,
    createdAt: Instant? = null,
    blockNumber: Long? = null,
    logIndex: Int? = null,
    minorLogIndex: Int? = null
) = copy(log = log.withNewValues(status, createdAt, blockNumber, logIndex, minorLogIndex))

fun ItemEvent.LazyItemBurnEvent.withNewValues(
    status: EthereumLogStatus? = null,
    createdAt: Instant? = null
) = copy(log = log.withNewValues(status, createdAt))

fun createRandomCreatorsItemEvent(): ItemEvent.ItemCreatorsEvent {
    val log = createRandomEthereumLog()
    val event = ItemEvent.ItemCreatorsEvent(
        creators = listOf(Part.fullPart(randomAddress()), Part.fullPart(randomAddress())),
        entityId = randomString(),
        log = log,
    )
    event.eventTimeMarks = indexerInNftBlockchainTimeMark(log)
    return event
}

fun createRandomTransferItemEvent(): ItemEvent.ItemTransferEvent {
    val log = createRandomEthereumLog()
    val event = ItemEvent.ItemTransferEvent(
        from = randomAddress(),
        to = randomAddress(),
        value = EthUInt256.of(randomInt()),
        entityId = randomString(),
        log = log,
    )
    event.eventTimeMarks = indexerInNftBlockchainTimeMark(log)
    return event
}

fun createRandomOpenSeaLazyItemMintEvent(): ItemEvent.OpenSeaLazyItemMintEvent {
    val log = createRandomEthereumLog()
    val event = ItemEvent.OpenSeaLazyItemMintEvent(
        from = randomAddress(),
        supply = EthUInt256.of(randomInt()),
        entityId = randomString(),
        log = log,
    )
    event.eventTimeMarks = indexerInNftBlockchainTimeMark(log)
    return event
}

fun createRandomMintItemEvent(
    transactionSender: Address = randomAddress()
): ItemEvent.ItemMintEvent {
    val log = createRandomEthereumLog(transactionSender = transactionSender)
    val event = ItemEvent.ItemMintEvent(
        owner = randomAddress(),
        supply = EthUInt256.of(randomInt()),
        entityId = randomString(),
        log = log,
    )
    event.eventTimeMarks = indexerInNftBlockchainTimeMark(log)
    return event
}

fun createRandomBurnItemEvent(): ItemEvent.ItemBurnEvent {
    val log = createRandomEthereumLog()
    val event = ItemEvent.ItemBurnEvent(
        supply = EthUInt256.of(randomInt()),
        owner = randomAddress(),
        entityId = randomString(),
        log = log,
    )
    event.eventTimeMarks = indexerInNftBlockchainTimeMark(log)
    return event
}

fun createRandomLazyMintItemEvent(): ItemEvent.LazyItemMintEvent {
    val log = createRandomEthereumLog()
    val event = ItemEvent.LazyItemMintEvent(
        supply = EthUInt256.of(randomInt()),
        creators = emptyList(),
        entityId = randomString(),
        log = log,
    )
    event.eventTimeMarks = indexerInNftBlockchainTimeMark(log)
    return event
}

fun createRandomLazyBurnItemEvent(): ItemEvent.LazyItemBurnEvent {
    val log = createRandomEthereumLog()
    val event = ItemEvent.LazyItemBurnEvent(
        supply = EthUInt256.of(randomInt()),
        entityId = randomString(),
        log = log,
    )
    event.eventTimeMarks = indexerInNftBlockchainTimeMark(log)
    return event
}

fun createRandomOwnershipTransferToEvent(
    owner: Address = randomAddress(),
    from: Address = randomAddress(),
    token: Address = randomAddress(),
    tokenId: EthUInt256 = EthUInt256.of(randomBigInt())
): OwnershipEvent.TransferToEvent {
    val entityId = OwnershipId(
        token = token,
        tokenId = tokenId,
        owner = owner
    )
    val log = createRandomEthereumLog()
    val event = OwnershipEvent.TransferToEvent(
        from = from,
        value = EthUInt256.of(randomInt()),
        entityId = entityId.stringValue,
        log = log,
    )
    event.eventTimeMarks = indexerInNftBlockchainTimeMark(log)
    return event
}

fun createRandomOwnershipId(): OwnershipId {
    return OwnershipId(randomAddress(), EthUInt256.of(randomBigInt()), randomAddress())
}

fun createRandomOwnership(
    token: Address = createAddress(),
    owner: Address = createAddress(),
    tokenId: EthUInt256 = EthUInt256.of(ThreadLocalRandom.current().nextLong(1, 10000))
): Ownership {
    return Ownership(
        token = token,
        tokenId = tokenId,
        creators = listOf(Part(AddressFactory.create(), 1000)),
        owner = owner,
        value = EthUInt256.of(BigInteger.valueOf(ThreadLocalRandom.current().nextLong(1, 10000))),
        lazyValue = EthUInt256.of(BigInteger.valueOf(ThreadLocalRandom.current().nextLong(1, 10000))),
        date = nowMillis(),
        pending = (1..ThreadLocalRandom.current().nextInt(1, 20)).map { createItemHistory() },
        lastUpdatedAt = nowMillis()
    )
}

fun createRandomOwnershipTransferFromEvent(): OwnershipEvent.TransferFromEvent {
    val log = createRandomEthereumLog()
    val event = OwnershipEvent.TransferFromEvent(
        to = randomAddress(),
        value = EthUInt256.of(randomInt()),
        entityId = randomString(),
        log = log,
    )
    event.eventTimeMarks = indexerInNftBlockchainTimeMark(log)
    return event
}

fun createRandomOwnershipChangeLazyValueEvent(): OwnershipEvent.ChangeLazyValueEvent {
    val log = createRandomEthereumLog()
    val event = OwnershipEvent.ChangeLazyValueEvent(
        value = EthUInt256.of(randomInt()),
        entityId = randomString(),
        log = log,
    )
    event.eventTimeMarks = indexerInNftBlockchainTimeMark(log)
    return event
}

fun createRandomOwnershipLazyTransferToEvent(): OwnershipEvent.LazyTransferToEvent {
    val log = createRandomEthereumLog()
    val event = OwnershipEvent.LazyTransferToEvent(
        value = EthUInt256.of(randomInt()),
        entityId = randomString(),
        log = log,
    )
    event.eventTimeMarks = indexerInNftBlockchainTimeMark(log)
    return event
}

fun createRandomOwnershipLazyBurnEvent(): OwnershipEvent.LazyBurnEvent {
    val log = createRandomEthereumLog()
    val event = OwnershipEvent.LazyBurnEvent(
        from = randomAddress(),
        value = EthUInt256.of(randomInt()),
        entityId = randomString(),
        log = log,
    )
    event.eventTimeMarks = indexerInNftBlockchainTimeMark(log)
    return event
}

fun OwnershipEvent.LazyTransferToEvent.withNewValues(
    status: EthereumLogStatus? = null,
    createdAt: Instant? = null,
    blockNumber: Long? = null
) = copy(log = log.withNewValues(status, createdAt, blockNumber))

fun OwnershipEvent.LazyBurnEvent.withNewValues(
    status: EthereumLogStatus? = null,
    createdAt: Instant? = null,
    blockNumber: Long? = null
) = copy(log = log.withNewValues(status, createdAt, blockNumber))

fun OwnershipEvent.TransferFromEvent.withNewValues(
    status: EthereumLogStatus? = null,
    createdAt: Instant? = null,
    blockNumber: Long? = null,
    logIndex: Int? = null,
    minorLogIndex: Int? = null
) = copy(log = log.withNewValues(status, createdAt, blockNumber, logIndex, minorLogIndex))

fun OwnershipEvent.TransferToEvent.withNewValues(
    status: EthereumLogStatus? = null,
    createdAt: Instant? = null,
    blockNumber: Long? = null,
    logIndex: Int? = null,
    minorLogIndex: Int? = null
) = copy(log = log.withNewValues(status, createdAt, blockNumber, logIndex, minorLogIndex))

fun randomEthereumLogEvent(): EthereumLog {
    val now = nowMillis()
    return EthereumLog(
        address = createAddress(),
        topic = Word.apply(randomWord()),
        transactionHash = randomWord(),
        index = RandomUtils.nextInt(),
        minorLogIndex = 0,
        status = EthereumLogStatus.CONFIRMED,
        blockHash = Word.apply(randomWord()),
        blockNumber = randomLong(),
        logIndex = randomInt(),
        visible = true,
        blockTimestamp = now.epochSecond,
        from = randomAddress(),
        to = randomAddress(),
        createdAt = now,
        updatedAt = now
    )
}

fun randomReversedLogRecord(data: EventData) = randomReversedLogRecord(data, randomEthereumLogEvent())

fun randomReversedLogRecord(data: EventData, log: EthereumLog): ReversedEthereumLogRecord {
    return ReversedEthereumLogRecord(
        id = randomString(),
        log = log,
        version = null,
        data = data
    )
}

fun createRandomBurnAction(): BurnItemAction {
    return BurnItemAction(
        token = randomAddress(),
        tokenId = EthUInt256.Companion.of(randomBigInt()),
        createdAt = nowMillis(),
        lastUpdatedAt = nowMillis(),
        state = ActionState.values().random(),
        actionAt = nowMillis()
    )
}

fun createRandomItemProperties(): ItemProperties {
    return ItemProperties(
        name = randomString(),
        description = randomString(),
        attributes = emptyList(),
        rawJsonContent = randomString(),
        content = ContentBuilder.getItemMetaContent(
            imageOriginal = randomString(),
            imageBig = randomString(),
            imagePreview = randomString(),
            videoOriginal = randomString()
        )
    )
}

fun createRandomBurnItemActionEvent(): BurnItemActionEvent {
    return BurnItemActionEvent(
        token = randomAddress(),
        tokenId = EthUInt256.of(randomBigInt()),
        burnAt = Instant.now()
    )
}

fun createRandomBurnItemAction(): BurnItemAction {
    return BurnItemAction(
        token = randomAddress(),
        tokenId = EthUInt256.of(randomBigInt()),
        actionAt = Instant.now(),
        lastUpdatedAt = Instant.now(),
        createdAt = Instant.now(),
        state = ActionState.values().random(),
        version = randomLong()
    )
}

fun randomTokenProperties(): TokenProperties {
    return TokenProperties(
        name = randomString(),
        description = randomString(),
        feeRecipient = randomAddress(),
        sellerFeeBasisPoints = randomInt(10000),
        createdAt = nowMillis(),
        tags = listOf(randomString(), randomString()),
        genres = listOf(randomString(), randomString()),
        language = randomString(2),
        rights = randomString(),
        rightsUri = randomString(),
        externalUri = randomString(),
        tokenUri = "http://localhost:8080/${randomString()}",
        content = ContentBuilder.getTokenMetaContent(
            imageOriginal = "http://test.com/${randomString()}"
        )
    )
}

fun createRandomInconsistentItem() = InconsistentItem(
    token = randomAddress(),
    tokenId = EthUInt256.of(randomWord()),
    status = InconsistentItemStatus.UNFIXED,
    fixVersionApplied = 1,
    lastUpdatedAt = nowMillis(),
    type = ItemProblemType.SUPPLY_MISMATCH,
    supply = EthUInt256.TEN,
    ownerships = EthUInt256.TEN,
    supplyValue = BigInteger.TEN,
    ownershipsValue = BigInteger.TEN,
)

fun randomUpdateSuspiciousItemsState(assetCount: Int = 10): UpdateSuspiciousItemsState {
    return UpdateSuspiciousItemsState(
        statedAt = Instant.now(),
        assets = (1..assetCount).map { randomUpdateSuspiciousItemsStateAsset() },
    )
}

fun randomUpdateSuspiciousItemsStateAsset(): UpdateSuspiciousItemsState.Asset {
    return UpdateSuspiciousItemsState.Asset(
        contract = randomAddress(),
        cursor = randomString()
    )
}

fun randomItemExState(id: ItemId = createRandomItemId()): ItemExState {
    return ItemExState(
        isSuspiciousOnOS = randomBoolean(),
        id = id
    )
}

