package com.rarible.protocol.order.listener.data

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomBinary
import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomWord
import com.rarible.looksrare.client.model.v2.CollectionType
import com.rarible.looksrare.client.model.v2.LooksrareEvent
import com.rarible.looksrare.client.model.v2.LooksrareEventType
import com.rarible.looksrare.client.model.v2.LooksrareOrder
import com.rarible.looksrare.client.model.v2.QuoteType
import com.rarible.looksrare.client.model.v2.Status
import io.daonomic.rpc.domain.Word
import scalether.domain.AddressFactory
import java.time.Duration
import java.time.Instant

fun randomLooksrareOrder(
    hash: Word = Word.apply(randomWord()),
    id: String = randomWord(),
): LooksrareOrder {
    return LooksrareOrder(
        hash = hash,
        collection = randomAddress(),
        itemIds = listOf(randomBigInt()),
        quoteType = QuoteType.ASK,
        signer = randomAddress(),
        strategyId = randomLong(),
        currency = randomAddress(),
        amounts = listOf(randomBigInt()),
        price = randomBigInt(),
        globalNonce = (1..1000).random().toBigInteger(),
        orderNonce = (1..1000).random().toBigInteger(),
        subsetNonce = (1..1000).random().toBigInteger(),
        startTime = Instant.now(),
        endTime = Instant.now() + Duration.ofHours(1),
        additionalParameters = randomBinary(),
        status = Status.values().random(),
        signature = randomBinary(),
        collectionType = CollectionType.values().random(),
        createdAt = Instant.now(),
        merkleRoot = randomBinary(),
        merkleProof = listOf(randomMerkleProof()),
        id = id
    )
}

fun randomMerkleProof(): com.rarible.looksrare.client.model.v2.MerkleProof {
    return com.rarible.looksrare.client.model.v2.MerkleProof(
        position = randomLong(),
        value = randomBinary()
    )
}

fun randomLooksrareCancelListEvent(
    id: String = randomWord(),
) = LooksrareEvent(
    id = id,
    createdAt = nowMillis(),
    from = AddressFactory.create(),
    type = LooksrareEventType.CANCEL_LIST,
    order = randomLooksrareOrder(),
)
