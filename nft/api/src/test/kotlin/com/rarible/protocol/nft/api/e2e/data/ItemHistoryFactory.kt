package com.rarible.protocol.nft.api.e2e.data

import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.EventData
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.nft.core.model.ItemLazyMint
import com.rarible.protocol.nft.core.model.ItemTransfer
import com.rarible.protocol.nft.core.model.Part
import com.rarible.protocol.nft.core.model.TokenStandard
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import org.apache.commons.lang3.RandomUtils
import scalether.domain.Address
import scalether.domain.AddressFactory
import java.time.Instant
import java.util.*
import java.util.concurrent.ThreadLocalRandom

fun LogEvent.withTransferOwner(owner: Address): LogEvent {
    val newData = (data as ItemTransfer).copy(owner = owner)
    return copy(data = newData)
}

fun LogEvent.withTransferFrom(from: Address): LogEvent {
    val newData = (data as ItemTransfer).copy(from = from)
    return copy(data = newData)
}

fun LogEvent.withToken(token: Address): LogEvent {
    val newData = (data as ItemTransfer).copy(token = token)
    return copy(data = newData)
}

fun LogEvent.withTokenId(tokenId: EthUInt256): LogEvent {
    val newData = (data as ItemTransfer).copy(tokenId = tokenId)
    return copy(data = newData)
}

fun LogEvent.getItemTransfer(): ItemTransfer {
    return data as ItemTransfer
}

fun LogEvent.withTransferDate(date: Instant): LogEvent {
    val newData = (data as ItemTransfer).copy(date = date)
    return copy(data = newData)
}

fun createItemMint(): LogEvent {
    return createItemTransfer()
        .withTransferOwner(AddressFactory.create())
        .withTransferFrom(Address.ZERO())
}

fun createItemBurn(): LogEvent {
    return createItemTransfer()
        .withTransferOwner(Address.ZERO())
        .withTransferFrom(AddressFactory.create())
}

fun createItemTransfer(): LogEvent {
    val data = ItemTransfer(
        owner = createAddress(),
        from = createAddress(),
        token = createAddress(),
        tokenId = EthUInt256.of(ThreadLocalRandom.current().nextLong(1, 2)),
        date = nowMillis(),
        value = EthUInt256.of(ThreadLocalRandom.current().nextLong(1, 10000))
    )
    return createLogEvent(data)
}

fun createItemTransfer(owner: Address = createAddress()) = ItemTransfer(
    owner = owner,
    token = createAddress(),
    tokenId = EthUInt256.of(ThreadLocalRandom.current().nextLong(1, 2)),
    date = nowMillis(),
    from = createAddress(),
    value = EthUInt256.of(ThreadLocalRandom.current().nextLong(1, 10000))
)

fun createItemLazyMint() = ItemLazyMint(
    token = createAddress(),
    tokenId = EthUInt256.of(ThreadLocalRandom.current().nextLong(1, 2)),
    uri = UUID.randomUUID().toString(),
    standard = listOf(TokenStandard.ERC1155, TokenStandard.ERC721).random(),
    date = nowMillis(),
    value = EthUInt256.of(ThreadLocalRandom.current().nextLong(1, 10000)),
    creators = listOf(Part(AddressFactory.create(), 5000), Part(AddressFactory.create(), 5000)),
    signatures = listOf(Binary.empty(), Binary.empty()),
    royalties = listOf(createPart(), createPart())
)

fun createLogEvent(data: EventData) = LogEvent(
    data = data,
    address = createAddress(),
    topic = Word.apply(RandomUtils.nextBytes(32)),
    transactionHash = Word.apply(RandomUtils.nextBytes(32)),
    index = RandomUtils.nextInt(),
    minorLogIndex = 0,
    status = LogEventStatus.CONFIRMED
)
