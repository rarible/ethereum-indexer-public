package com.rarible.protocol.order.core.model

import io.daonomic.rpc.domain.Word
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import scalether.domain.Address
import java.time.Instant

data class AuctionOffchainHistory(
    @Id
    val id: String = ObjectId().toString(),
    val hash: Word,
    val date: Instant,
    val contract: Address,
    val seller: Address,
    val sell: Asset,
    val source: HistorySource,
    val type: Type,
    val createdAt: Instant = Instant.now()
) {
    enum class Type {
        STARTED,
        ENDED
    }
}
