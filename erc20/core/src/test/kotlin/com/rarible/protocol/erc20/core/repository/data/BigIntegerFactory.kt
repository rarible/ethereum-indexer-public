package com.rarible.protocol.erc20.core.repository.data

import com.rarible.ethereum.domain.EthUInt256
import java.math.BigInteger
import java.util.concurrent.ThreadLocalRandom

fun createBigInteger(): BigInteger {
    return BigInteger.valueOf(ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE))
}

fun createEthUInt256(): EthUInt256 {
    return EthUInt256.of(createBigInteger())
}
