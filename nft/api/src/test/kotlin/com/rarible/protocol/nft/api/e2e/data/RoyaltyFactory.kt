package com.rarible.protocol.nft.api.e2e.data

import com.rarible.protocol.nft.core.model.Part
import java.util.concurrent.ThreadLocalRandom

fun createPart(): Part {
    return Part(
        account = createAddress(),
        value = ThreadLocalRandom.current().nextInt(1, 10000)
    )
}
