package com.rarible.protocol.order.core.repository.exchange

import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.order.core.model.*
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.index.Index

object AuctionHistoryRepositoryIndexes {

    val ALL_BY_TYPE_TOKEN_ID_DEFINITION: Index = Index()
        .on("${LogEvent::data.name}.${AuctionHistory::type.name}", Sort.Direction.ASC)
        .on("${LogEvent::data.name}.${Auction::sell.name}.${Asset::type.name}.${NftAssetType::token::name}", Sort.Direction.ASC)
        .on("${LogEvent::data.name}.${Auction::sell.name}.${Asset::type.name}.${NftAssetType::tokenId::name}", Sort.Direction.ASC)
        .background()

    val ALL_BY_TYPE_SELLER_DEFINITION: Index = Index()
        .on("${LogEvent::data.name}.${AuctionHistory::type.name}", Sort.Direction.ASC)
        .on("${LogEvent::data.name}.${OnChainAuction::seller.name}", Sort.Direction.ASC)
        .background()

    val ALL_BY_TYPE_BUYER_DEFINITION: Index = Index()
        .on("${LogEvent::data.name}.${AuctionHistory::type.name}", Sort.Direction.ASC)
        .on("${LogEvent::data.name}.${OnChainAuction::buyer.name}", Sort.Direction.ASC)
        .background()

    val ALL_INDEXES = listOf(
        ALL_BY_TYPE_TOKEN_ID_DEFINITION,
        ALL_BY_TYPE_SELLER_DEFINITION,
        ALL_BY_TYPE_BUYER_DEFINITION
    )
}
