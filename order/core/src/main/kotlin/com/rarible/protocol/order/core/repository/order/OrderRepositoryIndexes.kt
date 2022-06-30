package com.rarible.protocol.order.core.repository.order

import com.rarible.core.mongo.util.div
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.AssetType
import com.rarible.protocol.order.core.model.NftAssetType
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderOpenSeaV1DataV1
import com.rarible.protocol.order.core.model.OrderSeaportDataV1
import com.rarible.protocol.order.core.model.OrderStatus
import com.rarible.protocol.order.core.model.Platform
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.index.PartialIndexFilter
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.exists
import org.springframework.data.mongodb.core.query.isEqualTo

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

    // Best sell order of collection
    // TODO remove later
    val SELL_ORDERS_BY_COLLECTION_CURRENCY_SORT_BY_PRICE_DEFINITION = Index()
        .on("${Order::make.name}.${Asset::type.name}.${NftAssetType::token.name}", Sort.Direction.ASC)
        .on("${Order::take.name}.${Asset::type.name}.${NftAssetType::token.name}", Sort.Direction.ASC)
        .on(Order::makePrice.name, Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .partial(
            PartialIndexFilter.of(
                Criteria
                    .where(Order::status.name).isEqualTo(OrderStatus.ACTIVE)
                    .and(Order::make / Asset::type / AssetType::nft).isEqualTo(true)
            )
            )
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

    private val SELL_ORDERS_BY_COLLECTION_STATUS_DEFINITION = Index()
        .on("${Order::make.name}.${Asset::type.name}.${NftAssetType::nft.name}", Sort.Direction.ASC)
        .on("${Order::make.name}.${Asset::type.name}.${NftAssetType::token.name}", Sort.Direction.ASC)
        .on(Order::status.name, Sort.Direction.ASC)
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

    @Deprecated("Remove in release 1.27")
    val SELL_ORDERS_BY_MAKER_PLATFORM_DEFINITION = Index()
        .on("${Order::make.name}.${Asset::type.name}.${AssetType::nft.name}", Sort.Direction.ASC)
        .on(Order::maker.name, Sort.Direction.ASC)
        .on(Order::platform.name, Sort.Direction.ASC)
        .on(Order::lastUpdateAt.name, Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    val SELL_ORDERS_BY_MAKER_PLATFORM_STATUS_DEFINITION = Index()
        .on("${Order::make.name}.${Asset::type.name}.${AssetType::nft.name}", Sort.Direction.ASC)
        .on(Order::maker.name, Sort.Direction.ASC)
        .on(Order::platform.name, Sort.Direction.ASC)
        .on(Order::status.name, Sort.Direction.ASC)
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

    @Deprecated("Remove in release 1.27")
    val BIDS_BY_ITEM_PLATFORM_DEFINITION = Index()
        .on("${Order::take.name}.${Asset::type.name}.${NftAssetType::token.name}", Sort.Direction.ASC)
        .on("${Order::take.name}.${Asset::type.name}.${NftAssetType::tokenId.name}", Sort.Direction.ASC)
        .on(Order::platform.name, Sort.Direction.ASC)
        .on(Order::takePriceUsd.name, Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    // --------------------- getBidsByMaker ---------------------//
    // TODO these indices have 0 usage in prod, need to check them

    @Deprecated("Remove in release 1.27")
    val BIDS_BY_MAKER_DEFINITION = Index()
        .on("${Order::take.name}.${Asset::type.name}.${AssetType::nft.name}", Sort.Direction.ASC)
        .on(Order::maker.name, Sort.Direction.ASC)
        .on(Order::lastUpdateAt.name, Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    @Deprecated("Remove in release 1.27")
    val BIDS_BY_MAKER_PLATFORM_DEFINITION = Index()
        .on("${Order::take.name}.${Asset::type.name}.${AssetType::nft.name}", Sort.Direction.ASC)
        .on(Order::maker.name, Sort.Direction.ASC)
        .on(Order::platform.name, Sort.Direction.ASC)
        .on(Order::lastUpdateAt.name, Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    // --------------------- getAllOrders ---------------------//
    // TODO has 0 usage in prod, functionality can be covered by BY_LAST_UPDATE_AND_ID_DEFINITION
    @Deprecated("Remove in release 1.27")
    val BY_LAST_UPDATE_DEFINITION = Index()
        .on(Order::lastUpdateAt.name, Sort.Direction.ASC)
        .background()

    // TODO most probably should be removed - we're query all orders only with specified status (ACTIVE)
    val BY_LAST_UPDATE_AND_ID_DEFINITION = Index()
        .on(Order::lastUpdateAt.name, Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    private val BY_DB_UPDATE_UPDATE_DEFINITION = Index()
        .on(Order::dbUpdatedAt.name, Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    val BY_LAST_UPDATE_AND_STATUS_AND_ID_DEFINITION = Index()
        .on(Order::status.name, Sort.Direction.ASC)
        .on(Order::lastUpdateAt.name, Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    @Deprecated("Remove in release 1.27")
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

    // --------------------- for updating status by start/end ---------------------//
    val BY_PLATFORM_MAKER_AND_NONCE = Index()
        .on(Order::platform.name, Sort.Direction.ASC)
        .on(Order::maker.name, Sort.Direction.ASC)
        .on("${Order::data.name}.${OrderOpenSeaV1DataV1::nonce.name}", Sort.Direction.ASC)
        .partial(PartialIndexFilter.of(Order::data / OrderOpenSeaV1DataV1::nonce exists true))
        .background()

    val BY_STATUS_MAKER_AND_COUNTER = Index()
        .on(Order::status.name, Sort.Direction.ASC)
        .on(Order::maker.name, Sort.Direction.ASC)
        .on("${Order::data.name}.${OrderSeaportDataV1::counter.name}", Sort.Direction.ASC)
        .partial(PartialIndexFilter.of(Order::data / OrderSeaportDataV1::counter exists true))
        .background()

    // --------------------- Other ---------------------//

    val BY_BID_PLATFORM_STATUS_LAST_UPDATED_AT = Index()
        .on("${Order::take.name}.${Asset::type.name}.${AssetType::nft.name}", Sort.Direction.ASC)
        .on(Order::platform.name, Sort.Direction.ASC)
        .on(Order::status.name, Sort.Direction.ASC)
        .on(Order::lastUpdateAt.name, Sort.Direction.ASC)
        .partial(PartialIndexFilter.of(
            Criteria.where("${Order::take.name}.${Asset::type.name}.${AssetType::nft.name}").isEqualTo(true)
                .and(Order::platform.name).isEqualTo(Platform.RARIBLE)
        )
        )
        .background()

    val BY_MAKER_AND_STATUS_ONLY_SALE_ORDERS = Index()
        .on(Order::maker.name, Sort.Direction.ASC)
        .on(Order::status.name, Sort.Direction.ASC)
        .partial(PartialIndexFilter.of(Order::make / Asset::type / AssetType::nft isEqualTo true))
        .background()

    // Required for analytics/DWH PT-611
    val BY_CREATED_AT_AND_ID = Index()
        .on(Order::createdAt.name, Sort.Direction.ASC)
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
        SELL_ORDERS_BY_COLLECTION_CURRENCY_SORT_BY_PRICE_DEFINITION,

        SELL_ORDERS_BY_COLLECTION_DEFINITION,
        SELL_ORDERS_BY_COLLECTION_STATUS_DEFINITION,
        SELL_ORDERS_BY_COLLECTION_PLATFORM_DEFINITION,

        SELL_ORDERS_BY_MAKER_DEFINITION,
        SELL_ORDERS_BY_MAKER_PLATFORM_STATUS_DEFINITION,

        BIDS_BY_ITEM_DEFINITION_DEPRECATED,
        BIDS_BY_ITEM_DEFINITION,

        BY_LAST_UPDATE_AND_ID_DEFINITION,
        BY_DB_UPDATE_UPDATE_DEFINITION,
        BY_LAST_UPDATE_AND_STATUS_AND_ID_DEFINITION,

        BY_STATUS_AND_END_START,
        BY_PLATFORM_MAKER_AND_NONCE,
        BY_STATUS_MAKER_AND_COUNTER,

        BY_BID_PLATFORM_STATUS_LAST_UPDATED_AT,
        BY_MAKER_AND_STATUS_ONLY_SALE_ORDERS,
        BY_CREATED_AT_AND_ID
    )
}
