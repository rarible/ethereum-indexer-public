package com.rarible.protocol.nft.core.repository

import com.rarible.core.apm.CaptureSpan
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.Royalty
import com.rarible.protocol.nft.core.span.SpanType
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.count
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import scalether.domain.Address

@Component
@CaptureSpan(type = SpanType.DB, subtype = "royalty")
class RoyaltyRepository(
    private val mongo: ReactiveMongoOperations
) {

    fun save(royalty: Royalty): Mono<Royalty> {
        return mongo.save(royalty)
    }

    fun findByTokenAndId(address: Address, tokenId: EthUInt256): Mono<Royalty> {
        val query = Query(
            Criteria().andOperator(
                Royalty::address isEqualTo address,
                Royalty::tokenId isEqualTo tokenId
            )
        )
        return mongo.findOne<Royalty>(query, Royalty::class.java)
    }

    fun count(): Mono<Long> {
        return mongo.count<Royalty>()
    }
}
