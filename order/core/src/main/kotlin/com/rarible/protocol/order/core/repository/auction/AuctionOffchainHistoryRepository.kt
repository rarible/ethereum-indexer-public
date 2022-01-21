package com.rarible.protocol.order.core.repository.auction

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.AuctionOffchainHistory
import com.rarible.protocol.order.core.model.NftAssetType
import kotlinx.coroutines.reactive.awaitFirst
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux

@CaptureSpan(type = SpanType.DB)
@Component
class AuctionOffchainHistoryRepository(
    private val template: ReactiveMongoTemplate
) {

    val logger: Logger = LoggerFactory.getLogger(javaClass)

    suspend fun save(logEvent: AuctionOffchainHistory): AuctionOffchainHistory {
        return template.save(logEvent, COLLECTION).awaitFirst()
    }

    fun search(filter: ActivityAuctionOffchainFilter, size: Int): Flux<AuctionOffchainHistory> {
        val hint = filter.hint
        val criteria = filter.getCriteria()

        val query = Query(criteria).limit(size)

        if (hint != null) {
            query.withHint(hint)
        }
        return template.find(query.with(filter.sort), COLLECTION)
    }

    suspend fun createIndexes() {
        AuctionOffchainIndexes.ALL_INDEXES.forEach { index ->
            template.indexOps(AuctionHistoryRepository.COLLECTION).ensureIndex(index).awaitFirst()
        }
    }

    private object AuctionOffchainIndexes {
        val BY_LAST_UPDATE_AND_ID_DEFINITION: Index = Index()
            .on(AuctionOffchainHistory::type.name, Sort.Direction.ASC)
            .on(AuctionOffchainHistory::date.name, Sort.Direction.ASC)
            .on("_id", Sort.Direction.ASC)
            .background()

        val BY_TYPE_SELLER_DEFINITION: Index = Index()
            .on(AuctionOffchainHistory::type.name, Sort.Direction.ASC)
            .on("${AuctionOffchainHistory::seller.name}", Sort.Direction.ASC)
            .background()

        val BY_TYPE_TOKEN_ID_DEFINITION: Index = Index()
            .on(AuctionOffchainHistory::type.name, Sort.Direction.ASC)
            .on("${AuctionOffchainHistory::sell.name}.${Asset::type.name}.${NftAssetType::token::name}", Sort.Direction.ASC)
            .on("${AuctionOffchainHistory::sell.name}.${Asset::type.name}.${NftAssetType::tokenId::name}", Sort.Direction.ASC)
            .background()

        val ALL_INDEXES = listOf(
            BY_LAST_UPDATE_AND_ID_DEFINITION,
            BY_TYPE_SELLER_DEFINITION,
            BY_TYPE_TOKEN_ID_DEFINITION
        )
    }

    companion object {
        const val COLLECTION = "auction_offchain_history"
    }

}
