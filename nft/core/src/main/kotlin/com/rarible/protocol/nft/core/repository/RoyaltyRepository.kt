package com.rarible.protocol.nft.core.repository

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.Royalty
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.count
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import reactor.core.publisher.Mono
import scalether.domain.Address
import java.math.BigInteger


class RoyaltyRepository(
    private val mongo: ReactiveMongoOperations
) {

    fun save(royalty: Royalty): Mono<Royalty> {
        return mongo.save(royalty)
    }

    fun findByTokenAndId(address: Address, tokenId: EthUInt256): Mono<Royalty> {
        val query = Query().apply {
            addCriteria(Criteria.where("address").isEqualTo(address))
            addCriteria(Criteria.where("tokenId").isEqualTo(tokenId))
        }
        return mongo.findOne<Royalty>(query, Royalty::class.java)
    }

    fun count(): Mono<Long> {
        return mongo.count<Royalty>()
    }
}
