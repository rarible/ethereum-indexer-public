package com.rarible.protocol.nft.api.e2e.data

import com.rarible.protocol.dto.PartDto
import java.util.concurrent.ThreadLocalRandom

fun createPartDto(): PartDto {
    return PartDto(
        account = createAddress(),
        value = ThreadLocalRandom.current().nextInt(1, 10000)
    )
}
