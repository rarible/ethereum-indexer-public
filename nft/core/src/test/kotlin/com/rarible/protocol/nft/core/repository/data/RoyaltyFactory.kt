package com.rarible.protocol.nft.core.repository.data

import com.rarible.protocol.nft.core.model.Part
import java.util.concurrent.ThreadLocalRandom

fun createRoyalty(): Part {
    return Part(
        account = createAddress(),
        value = ThreadLocalRandom.current().nextInt(1, 10000)
    )
}
