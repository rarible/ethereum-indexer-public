package com.rarible.protocol.order.core.repository.auction

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.order.core.model.AuctionOffchainHistory
import kotlinx.coroutines.reactive.awaitFirst
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.stereotype.Component

@CaptureSpan(type = SpanType.DB)
@Component
class AuctionOffchainHistoryRepository(
    private val template: ReactiveMongoTemplate
) {

    val logger: Logger = LoggerFactory.getLogger(javaClass)

    suspend fun save(logEvent: AuctionOffchainHistory): AuctionOffchainHistory {
        return template.save(logEvent, COLLECTION).awaitFirst()
    }

    companion object {
        const val COLLECTION = "auction_offchain_history"
    }

}