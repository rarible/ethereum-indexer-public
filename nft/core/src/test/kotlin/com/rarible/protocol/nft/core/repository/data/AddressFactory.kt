package com.rarible.protocol.nft.core.repository.data

import scalether.domain.Address
import java.util.concurrent.ThreadLocalRandom

fun createAddress(): Address {
    val bytes = ByteArray(20)
    ThreadLocalRandom.current().nextBytes(bytes)
    return Address.apply(bytes)
}
