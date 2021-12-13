package com.rarible.protocol.order.core.repository.exchange

import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.order.core.model.*
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.index.Index

object ExchangeHistoryRepositoryIndexes {
    val ALL_SELL_DEFINITION: Index = Index()
        .on("${LogEvent::data.name}.${OrderExchangeHistory::make.name}.${Asset::type.name}.${AssetType::nft.name}", Sort.Direction.ASC)
        .on("${LogEvent::data.name}.${OrderExchangeHistory::date.name}", Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    val ALL_BID_DEFINITION: Index = Index()
        .on("${LogEvent::data.name}.${OrderExchangeHistory::take.name}.${Asset::type.name}.${AssetType::nft.name}", Sort.Direction.ASC)
        .on("${LogEvent::data.name}.${OrderExchangeHistory::date.name}", Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    val MAKER_SELL_DEFINITION: Index = Index()
        .on("${LogEvent::data.name}.${OrderExchangeHistory::make.name}.${Asset::type.name}.${AssetType::nft.name}", Sort.Direction.ASC)
        .on("${LogEvent::data.name}.${OrderExchangeHistory::maker.name}", Sort.Direction.ASC)
        .on("${LogEvent::data.name}.${OrderExchangeHistory::date.name}", Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    val MAKER_BID_DEFINITION: Index = Index()
        .on("${LogEvent::data.name}.${OrderExchangeHistory::take.name}.${Asset::type.name}.${AssetType::nft.name}", Sort.Direction.ASC)
        .on("${LogEvent::data.name}.${OrderExchangeHistory::maker.name}", Sort.Direction.ASC)
        .on("${LogEvent::data.name}.${OrderExchangeHistory::date.name}", Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    val TAKER_SELL_DEFINITION: Index = Index()
        .on("${LogEvent::data.name}.${OrderExchangeHistory::make.name}.${Asset::type.name}.${AssetType::nft.name}", Sort.Direction.ASC)
        .on("${LogEvent::data.name}.${OrderSideMatch::taker.name}", Sort.Direction.ASC)
        .on("${LogEvent::data.name}.${OrderExchangeHistory::date.name}", Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    val ITEM_SELL_DEFINITION: Index = Index()
        .on("${LogEvent::data.name}.${OrderExchangeHistory::make.name}.${Asset::type.name}.${NftAssetType::token.name}", Sort.Direction.ASC)
        .on("${LogEvent::data.name}.${OrderExchangeHistory::make.name}.${Asset::type.name}.${NftAssetType::tokenId.name}", Sort.Direction.ASC)
        .on("${LogEvent::data.name}.${OrderExchangeHistory::date.name}", Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    val ITEM_BID_DEFINITION: Index = Index()
        .on("${LogEvent::data.name}.${OrderExchangeHistory::take.name}.${Asset::type.name}.${NftAssetType::token.name}", Sort.Direction.ASC)
        .on("${LogEvent::data.name}.${OrderExchangeHistory::take.name}.${Asset::type.name}.${NftAssetType::tokenId.name}", Sort.Direction.ASC)
        .on("${LogEvent::data.name}.${OrderExchangeHistory::date.name}", Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    val COLLECTION_SELL_DEFINITION: Index = Index()
        .on("${LogEvent::data.name}.${OrderExchangeHistory::make.name}.${Asset::type.name}.${NftAssetType::nft.name}", Sort.Direction.ASC)
        .on("${LogEvent::data.name}.${OrderExchangeHistory::make.name}.${Asset::type.name}.${NftAssetType::token.name}", Sort.Direction.ASC)
        .on("${LogEvent::data.name}.${OrderExchangeHistory::date.name}", Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    val COLLECTION_BID_DEFINITION: Index = Index()
        .on("${LogEvent::data.name}.${OrderExchangeHistory::take.name}.${Asset::type.name}.${AssetType::nft.name}", Sort.Direction.ASC)
        .on("${LogEvent::data.name}.${OrderExchangeHistory::take.name}.${Asset::type.name}.${NftAssetType::token.name}", Sort.Direction.ASC)
        .on("${LogEvent::data.name}.${OrderExchangeHistory::date.name}", Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    val AGGREGATION_DEFINITION: Index = Index()
        .on("${LogEvent::data.name}.${OrderExchangeHistory::make.name}.${Asset::type.name}.${NftAssetType::nft.name}", Sort.Direction.ASC)
        .on("${LogEvent::data.name}.${OrderExchangeHistory::type.name}", Sort.Direction.ASC)
        .on("${LogEvent::data.name}.${OrderExchangeHistory::date.name}", Sort.Direction.ASC)
        .background()

    private val HASH_DEFINITION: Index = Index()
        .on("${LogEvent::data.name}.${OrderExchangeHistory::hash.name}", Sort.Direction.ASC)
        .on(LogEvent::blockNumber.name, Sort.Direction.ASC)
        .on(LogEvent::logIndex.name, Sort.Direction.ASC)
        .on(LogEvent::minorLogIndex.name, Sort.Direction.ASC)
        .background()

    val HASH_AND_SOURCE_DEFINITION: Index = Index()
        .on("${LogEvent::data.name}.${OrderExchangeHistory::hash.name}", Sort.Direction.ASC)
        .on("${LogEvent::data.name}.${OrderExchangeHistory::source.name}", Sort.Direction.ASC)
        .on(LogEvent::blockNumber.name, Sort.Direction.ASC)
        .on(LogEvent::logIndex.name, Sort.Direction.ASC)
        .on(LogEvent::minorLogIndex.name, Sort.Direction.ASC)
        .background()

    val ALL_INDEXES = listOf(
        ALL_SELL_DEFINITION,
        MAKER_SELL_DEFINITION,
        TAKER_SELL_DEFINITION,
        ITEM_SELL_DEFINITION,
        COLLECTION_SELL_DEFINITION,
        ALL_BID_DEFINITION,
        MAKER_BID_DEFINITION,
        ITEM_BID_DEFINITION,
        COLLECTION_BID_DEFINITION,
        AGGREGATION_DEFINITION,
        HASH_DEFINITION,
        HASH_AND_SOURCE_DEFINITION
    )
}
