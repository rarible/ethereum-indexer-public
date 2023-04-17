package com.rarible.protocol.order.listener.data

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomBinary
import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomWord
import com.rarible.looksrare.client.model.v2.CollectionType
import com.rarible.looksrare.client.model.v2.LooksrareOrder
import com.rarible.looksrare.client.model.v2.QuoteType
import com.rarible.looksrare.client.model.v2.Status
import io.daonomic.rpc.domain.Word
import java.time.Duration
import java.time.Instant

fun randomLooksrareOrder(): LooksrareOrder {
    return LooksrareOrder(
        hash = Word.apply(randomWord()),
        collection = randomAddress(),
        itemIds = listOf(randomBigInt()),
        quoteType  = QuoteType.ASK,
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
        id = randomWord()
    )
}

fun randomMerkleProof(): com.rarible.looksrare.client.model.v2.MerkleProof {
    return com.rarible.looksrare.client.model.v2.MerkleProof(
        position = randomLong(),
        value = randomBinary()
    )
}