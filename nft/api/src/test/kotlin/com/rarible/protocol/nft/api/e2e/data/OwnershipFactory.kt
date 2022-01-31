package com.rarible.protocol.nft.api.e2e.data

import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.Ownership
import scalether.domain.Address
import java.util.concurrent.ThreadLocalRandom

fun createOwnership(): Ownership {
    val token = createAddress()
    val owner = createAddress()
    val tokenId = EthUInt256.of(ThreadLocalRandom.current().nextLong(1, 10000))
    return Ownership(
        token = token,
        tokenId = tokenId,
        creators = listOf(createPart(), createPart()),
        owner = owner,
        value = EthUInt256.of(ThreadLocalRandom.current().nextLong(1, 10000)),
        lazyValue = EthUInt256.of(ThreadLocalRandom.current().nextLong(1, 10000)),
        date = nowMillis(),
        pending = (1..ThreadLocalRandom.current().nextInt(1, 20)).map { createItemTransfer().getItemTransfer() }
    )
}

fun createOwnership(
    token: Address,
    creator: Address?,
    owners: Address
) = createOwnership(token, EthUInt256.ONE, creator, owners)

fun createOwnership(
    token: Address,
    tokenId: EthUInt256,
    creator: Address?,
    owners: Address,
) = Ownership(
    token = token,
    tokenId = tokenId,
    value = EthUInt256.TEN,
    lazyValue = EthUInt256.TEN,
    creators = creator?.let { listOf(createPart().copy(account = it)) } ?: listOf(createPart(), createPart()),
    owner = owners,
    date = nowMillis(),
    pending = emptyList()
)
