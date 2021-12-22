package com.rarible.protocol.nft.core.repository

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.Royalty
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.count
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
@CaptureSpan(type = SpanType.DB)
class RoyaltyRepository(
    private val mongo: ReactiveMongoOperations
) {
    fun save(royalty: Royalty): Mono<Royalty> {
        return mongo.save(royalty)
    }

    fun findByItemId(itemId: ItemId): Mono<Royalty> {
        return mongo.findById(itemId, Royalty::class.java)
    }

    fun count(): Mono<Long> {
        return mongo.count<Royalty>()
    }
}
