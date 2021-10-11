package com.rarible.protocol.order.core.repository.order

import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.AssetType
import com.rarible.protocol.order.core.model.NftAssetType
import com.rarible.protocol.order.core.model.Order
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.index.Index

object OrderRepositoryIndexes {
    private val SELL_ORDERS_DEFINITION = Index()
        .on("${Order::make.name}.${Asset::type.name}.${AssetType::nft.name}", Sort.Direction.ASC)
        .on(Order::createdAt.name, Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    private val SELL_ORDERS_PLATFORM_DEFINITION = Index()
        .on("${Order::make.name}.${Asset::type.name}.${AssetType::nft.name}", Sort.Direction.ASC)
        .on(Order::platform.name, Sort.Direction.ASC)
        .on(Order::createdAt.name, Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    val SELL_ORDERS_BY_ITEM_DEFINITION_DEPRECATED = Index()
        .on("${Order::make.name}.${Asset::type.name}.${NftAssetType::token.name}", Sort.Direction.ASC)
        .on("${Order::make.name}.${Asset::type.name}.${NftAssetType::tokenId.name}", Sort.Direction.ASC)
        .on(Order::makePriceUsd.name, Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    val SELL_ORDERS_BY_ITEM_DEFINITION = Index()
        .on("${Order::make.name}.${Asset::type.name}.${NftAssetType::token.name}", Sort.Direction.ASC)
        .on("${Order::make.name}.${Asset::type.name}.${NftAssetType::tokenId.name}", Sort.Direction.ASC)
        .on(Order::makePrice.name, Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    private val SELL_ORDERS_BY_ITEM_PLATFORM_DEFINITION = Index()
        .on("${Order::make.name}.${Asset::type.name}.${NftAssetType::token.name}", Sort.Direction.ASC)
        .on("${Order::make.name}.${Asset::type.name}.${NftAssetType::tokenId.name}", Sort.Direction.ASC)
        .on(Order::platform.name, Sort.Direction.ASC)
        .on(Order::makePriceUsd.name, Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    private val SELL_ORDERS_BY_COLLECTION_DEFINITION = Index()
        .on("${Order::make.name}.${Asset::type.name}.${NftAssetType::nft.name}", Sort.Direction.ASC)
        .on("${Order::make.name}.${Asset::type.name}.${NftAssetType::token.name}", Sort.Direction.ASC)
        .on(Order::lastUpdateAt.name, Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    private val SELL_ORDERS_BY_COLLECTION_PLATFORM_DEFINITION = Index()
        .on("${Order::make.name}.${Asset::type.name}.${NftAssetType::nft.name}", Sort.Direction.ASC)
        .on("${Order::make.name}.${Asset::type.name}.${NftAssetType::token.name}", Sort.Direction.ASC)
        .on(Order::platform.name, Sort.Direction.ASC)
        .on(Order::lastUpdateAt.name, Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    private val SELL_ORDERS_BY_MAKER_DEFINITION = Index()
        .on("${Order::make.name}.${Asset::type.name}.${AssetType::nft.name}", Sort.Direction.ASC)
        .on(Order::maker.name, Sort.Direction.ASC)
        .on(Order::lastUpdateAt.name, Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    private val SELL_ORDERS_BY_MAKER_PLATFORM_DEFINITION = Index()
        .on("${Order::make.name}.${Asset::type.name}.${AssetType::nft.name}", Sort.Direction.ASC)
        .on(Order::maker.name, Sort.Direction.ASC)
        .on(Order::platform.name, Sort.Direction.ASC)
        .on(Order::lastUpdateAt.name, Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    val BIDS_BY_ITEM_DEFINITION_DEPRECATED = Index()
        .on("${Order::take.name}.${Asset::type.name}.${NftAssetType::token.name}", Sort.Direction.ASC)
        .on("${Order::take.name}.${Asset::type.name}.${NftAssetType::tokenId.name}", Sort.Direction.ASC)
        .on(Order::takePriceUsd.name, Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    val BIDS_BY_ITEM_DEFINITION = Index()
        .on("${Order::take.name}.${Asset::type.name}.${NftAssetType::token.name}", Sort.Direction.ASC)
        .on("${Order::take.name}.${Asset::type.name}.${NftAssetType::tokenId.name}", Sort.Direction.ASC)
        .on(Order::takePrice.name, Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    private val BIDS_BY_ITEM_PLATFORM_DEFINITION = Index()
        .on("${Order::take.name}.${Asset::type.name}.${NftAssetType::token.name}", Sort.Direction.ASC)
        .on("${Order::take.name}.${Asset::type.name}.${NftAssetType::tokenId.name}", Sort.Direction.ASC)
        .on(Order::platform.name, Sort.Direction.ASC)
        .on(Order::takePriceUsd.name, Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    private val BIDS_BY_MAKER_DEFINITION = Index()
        .on("${Order::take.name}.${Asset::type.name}.${AssetType::nft.name}", Sort.Direction.ASC)
        .on(Order::maker.name, Sort.Direction.ASC)
        .on(Order::lastUpdateAt.name, Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    private val BIDS_BY_MAKER_PLATFORM_DEFINITION = Index()
        .on("${Order::take.name}.${Asset::type.name}.${AssetType::nft.name}", Sort.Direction.ASC)
        .on(Order::maker.name, Sort.Direction.ASC)
        .on(Order::platform.name, Sort.Direction.ASC)
        .on(Order::lastUpdateAt.name, Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    private val BY_LAST_UPDATE_DEFINITION = Index()
        .on(Order::lastUpdateAt.name, Sort.Direction.ASC)
        .background()

    val BY_LAST_UPDATE_AND_ID_DEFINITION = Index()
        .on(Order::lastUpdateAt.name, Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    val BY_MAKE_STOCK_DEFINITION = Index()
        // orders with non-zero makeStock should be first
        .on(Order::makeStock.name, Sort.Direction.ASC)
        // recently updated orders should be first
        .on(Order::lastUpdateAt.name, Sort.Direction.ASC)
        // for queries with continuation
        .on("_id", Sort.Direction.ASC)
        .background()

    val BY_END_START_AND_MAKE_STOCK = Index()
        .on(Order::end.name, Sort.Direction.ASC)
        .on(Order::start.name, Sort.Direction.ASC)
        .on(Order::makeStock.name, Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC) // ?
        .background()

    val ALL_INDEXES = listOf(
        SELL_ORDERS_DEFINITION,
        SELL_ORDERS_PLATFORM_DEFINITION,

        SELL_ORDERS_BY_ITEM_DEFINITION_DEPRECATED,
        SELL_ORDERS_BY_ITEM_DEFINITION,
        SELL_ORDERS_BY_ITEM_PLATFORM_DEFINITION,

        SELL_ORDERS_BY_COLLECTION_DEFINITION,
        SELL_ORDERS_BY_COLLECTION_PLATFORM_DEFINITION,

        SELL_ORDERS_BY_MAKER_DEFINITION,
        SELL_ORDERS_BY_MAKER_PLATFORM_DEFINITION,

        BIDS_BY_ITEM_DEFINITION_DEPRECATED,
        BIDS_BY_ITEM_DEFINITION,
        BIDS_BY_ITEM_PLATFORM_DEFINITION,

        BIDS_BY_MAKER_DEFINITION,
        BIDS_BY_MAKER_PLATFORM_DEFINITION,

        BY_LAST_UPDATE_DEFINITION,
        BY_MAKE_STOCK_DEFINITION,
        BY_LAST_UPDATE_AND_ID_DEFINITION,

        BY_END_START_AND_MAKE_STOCK
    )
}
