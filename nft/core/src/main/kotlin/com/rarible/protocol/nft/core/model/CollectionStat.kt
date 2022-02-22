package com.rarible.protocol.nft.core.model

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.mapping.Document
import scalether.domain.Address
import java.math.BigInteger
import java.time.Instant

@Document("collection_stat")
data class CollectionStat(
    @Id
    val id: Address,
    val lastUpdatedAt: Instant,
    val totalItemSupply: BigInteger,
    val totalOwnerCount: Int,

    @Version
    val version: Long? = null,
) {

    companion object {

        fun empty(token: Address): CollectionStat {
            return CollectionStat(
                id = token,
                lastUpdatedAt = Instant.EPOCH,
                totalItemSupply = BigInteger.ZERO,
                totalOwnerCount = 0,
                version = null
            )
        }
    }

}