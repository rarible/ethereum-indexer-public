package com.rarible.protocol.order.core.model

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.mapping.Document
import scalether.domain.Address
import java.math.BigDecimal
import java.time.Instant

@Document("collection_stat")
data class CollectionStat(
    @Id
    val id: Address,
    val lastUpdatedAt: Instant,
    val floorPrice: BigDecimal,
    val totalVolume: BigDecimal,
    val highestSale: BigDecimal,

    @Version
    val version: Long? = null,
) {

    companion object {

        fun empty(token: Address): CollectionStat {
            return CollectionStat(
                id = token,
                lastUpdatedAt = Instant.EPOCH,
                floorPrice = BigDecimal.ZERO,
                totalVolume = BigDecimal.ZERO,
                highestSale = BigDecimal.ZERO,
                version = null
            )
        }
    }

}