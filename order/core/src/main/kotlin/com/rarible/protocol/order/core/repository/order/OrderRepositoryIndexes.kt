package com.rarible.protocol.order.core.repository.order

import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.AssetType
import com.rarible.protocol.order.core.model.NftAssetType
import com.rarible.protocol.order.core.model.Order
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.index.Index

object OrderRepositoryIndexes {

    // --------------------- getSellOrders ---------------------//
    val SELL_ORDERS_DEFINITION = Index()
        .on("${Order::make.name}.${Asset::type.name}.${AssetType::nft.name}", Sort.Direction.ASC)
        .on(Order::lastUpdateAt.name, Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    val SELL_ORDERS_PLATFORM_DEFINITION = Index()
        .on("${Order::make.name}.${Asset::type.name}.${AssetType::nft.name}", Sort.Direction.ASC)
        .on(Order::platform.name, Sort.Direction.ASC)
        .on(Order::lastUpdateAt.name, Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    val SELL_ORDERS_STATUS_DEFINITION = Index()
        .on("${Order::make.name}.${Asset::type.name}.${AssetType::nft.name}", Sort.Direction.ASC)
        .on(Order::status.name, Sort.Direction.ASC)
        .on(Order::lastUpdateAt.name, Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    val SELL_ORDERS_PLATFORM_STATUS_DEFINITION = Index()
        .on("${Order::make.name}.${Asset::type.name}.${AssetType::nft.name}", Sort.Direction.ASC)
        .on(Order::platform.name, Sort.Direction.ASC)
        .on(Order::status.name, Sort.Direction.ASC)
        .on(Order::lastUpdateAt.name, Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    // --------------------- getSellOrdersByItem ---------------------//
    //TODO for some reason this index heavily used in prod
    val SELL_ORDERS_BY_ITEM_SORT_BY_USD_PRICE_DEFINITION = Index()
        .on("${Order::make.name}.${Asset::type.name}.${NftAssetType::token.name}", Sort.Direction.ASC)
        .on("${Order::make.name}.${Asset::type.name}.${NftAssetType::tokenId.name}", Sort.Direction.ASC)
        .on(Order::makePriceUsd.name, Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    val SELL_ORDERS_BY_ITEM_SORT_BY_PRICE_DEFINITION = Index()
        .on("${Order::make.name}.${Asset::type.name}.${NftAssetType::token.name}", Sort.Direction.ASC)
        .on("${Order::make.name}.${Asset::type.name}.${NftAssetType::tokenId.name}", Sort.Direction.ASC)
        .on(Order::makePrice.name, Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    val SELL_ORDERS_BY_ITEM_PLATFORM_SORT_BY_USD_PRICE_DEFINITION = Index()
        .on("${Order::make.name}.${Asset::type.name}.${NftAssetType::token.name}", Sort.Direction.ASC)
        .on("${Order::make.name}.${Asset::type.name}.${NftAssetType::tokenId.name}", Sort.Direction.ASC)
        .on(Order::platform.name, Sort.Direction.ASC)
        .on(Order::makePriceUsd.name, Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    // Best sell order by status
    val SELL_ORDERS_BY_ITEM_CURRENCY_STATUS_SORT_BY_PRICE_DEFINITION = Index()
        .on("${Order::make.name}.${Asset::type.name}.${NftAssetType::token.name}", Sort.Direction.ASC)
        .on("${Order::make.name}.${Asset::type.name}.${NftAssetType::tokenId.name}", Sort.Direction.ASC)
        .on("${Order::take.name}.${Asset::type.name}.${NftAssetType::token.name}", Sort.Direction.ASC)
        .on(Order::status.name, Sort.Direction.ASC)
        .on(Order::makePrice.name, Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    // Best sell order by ownership (used by Union to find best sell order for ownership)
    val SELL_ORDERS_BY_ITEM_MAKER_SORT_BY_PRICE_DEFINITION = Index()
        .on("${Order::make.name}.${Asset::type.name}.${NftAssetType::token.name}", Sort.Direction.ASC)
        .on("${Order::make.name}.${Asset::type.name}.${NftAssetType::tokenId.name}", Sort.Direction.ASC)
        .on(Order::maker.name, Sort.Direction.ASC)
        .on(Order::makePrice.name, Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    // --------------------- getSellOrdersByCollection ---------------------//
    val SELL_ORDERS_BY_COLLECTION_DEFINITION = Index()
        .on("${Order::make.name}.${Asset::type.name}.${NftAssetType::nft.name}", Sort.Direction.ASC)
        .on("${Order::make.name}.${Asset::type.name}.${NftAssetType::token.name}", Sort.Direction.ASC)
        .on(Order::lastUpdateAt.name, Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    val SELL_ORDERS_BY_COLLECTION_PLATFORM_DEFINITION = Index()
        .on("${Order::make.name}.${Asset::type.name}.${NftAssetType::nft.name}", Sort.Direction.ASC)
        .on("${Order::make.name}.${Asset::type.name}.${NftAssetType::token.name}", Sort.Direction.ASC)
        .on(Order::platform.name, Sort.Direction.ASC)
        .on(Order::lastUpdateAt.name, Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    // --------------------- getSellOrdersByMaker ---------------------//
    val SELL_ORDERS_BY_MAKER_DEFINITION = Index()
        .on("${Order::make.name}.${Asset::type.name}.${AssetType::nft.name}", Sort.Direction.ASC)
        .on(Order::maker.name, Sort.Direction.ASC)
        .on(Order::lastUpdateAt.name, Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    val SELL_ORDERS_BY_MAKER_PLATFORM_DEFINITION = Index()
        .on("${Order::make.name}.${Asset::type.name}.${AssetType::nft.name}", Sort.Direction.ASC)
        .on(Order::maker.name, Sort.Direction.ASC)
        .on(Order::platform.name, Sort.Direction.ASC)
        .on(Order::lastUpdateAt.name, Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    // --------------------- getBidsByItem ---------------------//
    //
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

    val BIDS_BY_ITEM_PLATFORM_DEFINITION = Index()
        .on("${Order::take.name}.${Asset::type.name}.${NftAssetType::token.name}", Sort.Direction.ASC)
        .on("${Order::take.name}.${Asset::type.name}.${NftAssetType::tokenId.name}", Sort.Direction.ASC)
        .on(Order::platform.name, Sort.Direction.ASC)
        .on(Order::takePriceUsd.name, Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    // --------------------- getBidsByMaker ---------------------//
    // TODO these indices have 0 usage in prod, need to check them

    val BIDS_BY_MAKER_DEFINITION = Index()
        .on("${Order::take.name}.${Asset::type.name}.${AssetType::nft.name}", Sort.Direction.ASC)
        .on(Order::maker.name, Sort.Direction.ASC)
        .on(Order::lastUpdateAt.name, Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    val BIDS_BY_MAKER_PLATFORM_DEFINITION = Index()
        .on("${Order::take.name}.${Asset::type.name}.${AssetType::nft.name}", Sort.Direction.ASC)
        .on(Order::maker.name, Sort.Direction.ASC)
        .on(Order::platform.name, Sort.Direction.ASC)
        .on(Order::lastUpdateAt.name, Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    // --------------------- getAllOrders ---------------------//
    // TODO has 0 usage in prod, functionality can be covered by BY_LAST_UPDATE_AND_ID_DEFINITION
    private val BY_LAST_UPDATE_DEFINITION = Index()
        .on(Order::lastUpdateAt.name, Sort.Direction.ASC)
        .background()

    // TODO most probably should be removed - we're query all orders only with specified status (ACTIVE)
    val BY_LAST_UPDATE_AND_ID_DEFINITION = Index()
        .on(Order::lastUpdateAt.name, Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    val BY_LAST_UPDATE_AND_STATUS_AND_ID_DEFINITION = Index()
        .on(Order::status.name, Sort.Direction.ASC)
        .on(Order::lastUpdateAt.name, Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    val BY_LAST_UPDATE_AND_STATUS_AND_PLATFORM_AND_ID_DEFINITION = Index()
        .on(Order::platform.name, Sort.Direction.ASC)
        .on(Order::status.name, Sort.Direction.ASC)
        .on(Order::lastUpdateAt.name, Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    // --------------------- for updating status by start/end ---------------------//
    val BY_STATUS_AND_END_START = Index()
        .on(Order::status.name, Sort.Direction.ASC)
        .on(Order::end.name, Sort.Direction.ASC)
        .on(Order::start.name, Sort.Direction.ASC)
        .background()

    // --------------------- Other ---------------------//
    // TODO these indices have 0 usage in prod, need to check them

    val BY_MAKE_STOCK_DEFINITION = Index()
        // orders with non-zero makeStock should be first
        .on(Order::makeStock.name, Sort.Direction.ASC)
        // recently updated orders should be first
        .on(Order::lastUpdateAt.name, Sort.Direction.ASC)
        // for queries with continuation
        .on("_id", Sort.Direction.ASC)
        .background()

    val ALL_INDEXES = listOf(
        SELL_ORDERS_DEFINITION,
        SELL_ORDERS_PLATFORM_DEFINITION,
        SELL_ORDERS_STATUS_DEFINITION,
        SELL_ORDERS_PLATFORM_STATUS_DEFINITION,

        SELL_ORDERS_BY_ITEM_SORT_BY_USD_PRICE_DEFINITION,
        SELL_ORDERS_BY_ITEM_SORT_BY_PRICE_DEFINITION,
        SELL_ORDERS_BY_ITEM_PLATFORM_SORT_BY_USD_PRICE_DEFINITION,
        SELL_ORDERS_BY_ITEM_CURRENCY_STATUS_SORT_BY_PRICE_DEFINITION,
        SELL_ORDERS_BY_ITEM_MAKER_SORT_BY_PRICE_DEFINITION,

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
        BY_LAST_UPDATE_AND_ID_DEFINITION,
        BY_LAST_UPDATE_AND_STATUS_AND_ID_DEFINITION,
        BY_LAST_UPDATE_AND_STATUS_AND_PLATFORM_AND_ID_DEFINITION,

        BY_MAKE_STOCK_DEFINITION,
        BY_STATUS_AND_END_START
    )
}
