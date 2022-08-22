package com.rarible.protocol.order.listener.data

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomBinary
import com.rarible.core.test.data.randomBoolean
import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomWord
import com.rarible.looksrare.client.model.v1.LooksrareOrder
import com.rarible.looksrare.client.model.v1.Status
import io.daonomic.rpc.domain.Word
import java.time.Duration
import java.time.Instant

fun randomLooksrareOrder(): LooksrareOrder {
    return LooksrareOrder(
        hash = Word.apply(randomWord()),
        collectionAddress = randomAddress(),
        tokenId = randomBigInt(),
        isOrderAsk  = randomBoolean(),
        signer = randomAddress(),
        strategy = randomAddress(),
        currencyAddress = randomAddress(),
        amount = randomBigInt(),
        price = randomBigInt(),
        nonce = (1..1000).random().toBigInteger(),
        startTime = Instant.now(),
        endTime = Instant.now() + Duration.ofHours(1),
        minPercentageToAsk = randomInt(),
        params = randomBinary(),
        status = Status.values().random(),
        signature =  randomBinary(),
        v = null,
        r = null,
        s = null
    )
}