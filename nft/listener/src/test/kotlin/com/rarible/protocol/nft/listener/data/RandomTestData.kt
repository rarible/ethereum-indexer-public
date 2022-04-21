package com.rarible.protocol.nft.listener.data

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomString
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.Part
import scalether.domain.AddressFactory
import java.math.BigInteger
import java.util.concurrent.ThreadLocalRandom

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
        pending = emptyList()
    )
}

fun createRandomItemProperties(): ItemProperties {
    return ItemProperties(
        name = randomString(),
        description = randomString(),
        image = randomString(),
        imagePreview = randomString(),
        imageBig = randomString(),
        animationUrl = randomString(),
        attributes = emptyList(),
        rawJsonContent = randomString()
    )
}
