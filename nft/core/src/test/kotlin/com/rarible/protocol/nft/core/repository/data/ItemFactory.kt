package com.rarible.protocol.nft.core.repository.data

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomBigInt
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.data.createRandomItemId
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.Part
import scalether.domain.Address
import scalether.domain.AddressFactory
import java.math.BigInteger
import java.util.concurrent.ThreadLocalRandom

fun createItem(
    token: Address = createAddress(),
    tokenId: EthUInt256 = EthUInt256.of(ThreadLocalRandom.current().nextLong(1, 10000))
): Item {
    return Item(
        token = token,
        tokenId = tokenId,
        creators = listOf(Part(AddressFactory.create(), 1000)),
        supply = EthUInt256.of(BigInteger.valueOf(ThreadLocalRandom.current().nextLong(1, 10000))),
        lazySupply = EthUInt256.of(BigInteger.valueOf(ThreadLocalRandom.current().nextLong(1, 10000))),
        royalties = (1..ThreadLocalRandom.current().nextInt(1, 20)).map { createRoyalty() },
        owners = (1..ThreadLocalRandom.current().nextInt(1, 20)).map { createAddress() },
        date = nowMillis(),
        isRaribleContract = false
    )
}

fun createItem(
    token: Address = createAddress(),
    tokenId: BigInteger = randomBigInt()
): Item {
    return createItem(token, EthUInt256.of(tokenId))
}

fun createItem(itemId: ItemId = createRandomItemId()): Item {
    return createItem(itemId.token, itemId.tokenId)
}
