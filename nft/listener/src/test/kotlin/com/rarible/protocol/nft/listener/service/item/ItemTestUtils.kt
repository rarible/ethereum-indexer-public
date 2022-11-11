package com.rarible.protocol.nft.listener.service.item

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomWord
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemTransfer
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.listener.data.createRandomOwnership
import io.daonomic.rpc.domain.Word
import io.daonomic.rpc.domain.WordFactory
import scalether.domain.Address

const val OWNERS_NUMBER = 4

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
