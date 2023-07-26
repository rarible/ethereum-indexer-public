package com.rarible.protocol.nft.listener.test.data

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomBoolean
import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomString
import com.rarible.core.test.data.randomWord
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.opensea.client.model.v1.Asset
import com.rarible.opensea.client.model.v1.AssetContract
import com.rarible.opensea.client.model.v1.AssetSchema
import com.rarible.opensea.client.model.v1.OpenSeaAssets
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.model.ItemTransfer
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.Part
import com.rarible.protocol.nft.core.service.item.meta.properties.ContentBuilder
import io.daonomic.rpc.domain.Word
import io.daonomic.rpc.domain.WordFactory
import scalether.domain.Address
import scalether.domain.AddressFactory
import java.math.BigInteger
import java.util.concurrent.ThreadLocalRandom

const val OWNERS_NUMBER = 4

fun createRandomItem(): Item {
    return Item.empty(randomAddress(), EthUInt256.of(randomLong()))
}

fun createRandomOwnership(): Ownership {
    val token = randomAddress()
    val owner = randomAddress()
    val tokenId = EthUInt256.of(ThreadLocalRandom.current().nextLong(1, 10000))
    return Ownership(
        token = token,
        tokenId = tokenId,
        creators = listOf(Part(AddressFactory.create(), 1000)),
        owner = owner,
        value = EthUInt256.of(BigInteger.valueOf(ThreadLocalRandom.current().nextLong(1, 10000))),
        lazyValue = EthUInt256.of(BigInteger.valueOf(ThreadLocalRandom.current().nextLong(1, 10000))),
        date = nowMillis(),
        pending = emptyList(),
        lastUpdatedAt = nowMillis()
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

fun createValidLog(item: Item, ownerships: List<Ownership>): List<LogEvent> {
    return ownerships.mapIndexed { index, ownership ->
        createLog(
            token = item.token,
            tokenId = item.tokenId,
            value = EthUInt256.ONE,
            from = Address.ZERO(),
            owner = ownership.owner,
            logIndex = index
        )
    }
}

fun createValidOwnerships(item: Item): List<Ownership> {
    return (1..OWNERS_NUMBER).map {
        createRandomOwnership().copy(
            token = item.token,
            tokenId = item.tokenId,
            value = item.supply / EthUInt256.of(OWNERS_NUMBER)
        )
    }
}

fun createInvalidValidOwnerships(item: Item): List<Ownership> {
    return (1..OWNERS_NUMBER).map {
        createRandomOwnership().copy(
            token = item.token,
            tokenId = item.tokenId,
            value = item.supply
        )
    }
}

fun createLog(
    token: Address = randomAddress(),
    blockNumber: Long = 1,
    tokenId: EthUInt256 = EthUInt256.of(randomBigInt()),
    value: EthUInt256 = EthUInt256.ONE,
    owner: Address = randomAddress(),
    from: Address = Address.ZERO(),
    logIndex: Int
): LogEvent {
    val transfer = ItemTransfer(
        owner = owner,
        token = token,
        tokenId = tokenId,
        date = nowMillis(),
        from = from,
        value = value
    )
    return LogEvent(
        data = transfer,
        address = token,
        topic = WordFactory.create(),
        transactionHash = Word.apply(randomWord()),
        status = LogEventStatus.CONFIRMED,
        from = randomAddress(),
        index = 0,
        logIndex = logIndex,
        blockNumber = blockNumber,
        minorLogIndex = 0,
        blockTimestamp = nowMillis().epochSecond,
        createdAt = nowMillis()
    )
}

fun randomOpenSeaAssets(
    assets: List<Asset> = (1..10).map { randomOpenSeaAsset() }
): OpenSeaAssets {
    return OpenSeaAssets(
        assets = assets,
        next = randomString(),
        previous = randomString()

    )
}

fun randomOpenSeaAsset(): Asset {
    return Asset(
        id = randomBigInt(),
        assetContract = AssetContract(
            randomAddress(),
            AssetSchema.values().random()
        ),
        supportsWyvern = randomBoolean(),
        tokenId = randomBigInt()
    )
}
