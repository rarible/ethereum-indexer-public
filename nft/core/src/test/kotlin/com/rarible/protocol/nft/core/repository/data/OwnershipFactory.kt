package com.rarible.protocol.nft.core.repository.data

import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.Part
import scalether.domain.AddressFactory
import java.math.BigInteger
import java.util.concurrent.ThreadLocalRandom

fun createOwnership(): Ownership {
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
