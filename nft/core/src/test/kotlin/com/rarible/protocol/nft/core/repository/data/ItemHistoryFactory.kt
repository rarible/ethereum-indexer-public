package com.rarible.protocol.nft.core.repository.data

import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.ItemTransfer
import java.math.BigInteger
import java.util.concurrent.ThreadLocalRandom

fun createItemHistory(): ItemTransfer {
    return ItemTransfer(
        owner = createAddress(),
        token = createAddress(),
        tokenId = EthUInt256.of(ThreadLocalRandom.current().nextLong(1, 2)),
        date = nowMillis(),
        from = createAddress(),
        value = EthUInt256.of(BigInteger.valueOf(ThreadLocalRandom.current().nextLong(1, 10000)))
    )
}
