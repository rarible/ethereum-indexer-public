package com.rarible.protocol.order.core.repository.order

import com.rarible.core.mongo.util.div
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.AssetType
import com.rarible.protocol.order.core.model.NftAssetType
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderLooksrareDataV2
import com.rarible.protocol.order.core.model.OrderStatus
import com.rarible.protocol.order.core.model.Platform
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.index.PartialIndexFilter
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.isEqualTo

object OrderRepositoryIndexes {

    // --------------------- getSellOrders ---------------------//

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
    // TODO for some reason this index heavily used in prod
    // TODO ideally we should get rid of sorting by USD and make 'currency' param mandatory
    val SELL_ORDERS_BY_ITEM_SORT_BY_USD_PRICE_DEFINITION = Index()
        .on("${Order::make.name}.${Asset::type.name}.${NftAssetType::token.name}", Sort.Direction.ASC)
        .on("${Order::make.name}.${Asset::type.name}.${NftAssetType::tokenId.name}", Sort.Direction.ASC)
        .on(Order::makePriceUsd.name, Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    // TODO ideally we should get rid of sorting by USD and make 'currency' param mandatory
    val SELL_ORDERS_BY_ITEM_PLATFORM_SORT_BY_USD_PRICE_DEFINITION = Index()
        .on("${Order::make.name}.${Asset::type.name}.${NftAssetType::token.name}", Sort.Direction.ASC)
        .on("${Order::make.name}.${Asset::type.name}.${NftAssetType::tokenId.name}", Sort.Direction.ASC)
        .on(Order::platform.name, Sort.Direction.ASC)
        .on(Order::makePriceUsd.name, Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    val SELL_ORDERS_BY_ITEM_SORT_BY_PRICE_DEFINITION = Index()
        .on("${Order::make.name}.${Asset::type.name}.${NftAssetType::token.name}", Sort.Direction.ASC)
        .on("${Order::make.name}.${Asset::type.name}.${NftAssetType::tokenId.name}", Sort.Direction.ASC)
        .on(Order::makePrice.name, Sort.Direction.ASC)
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

    val SELL_ORDERS_BY_CURRENCY_COLLECTION_DEFINITION = Index()
        .on("${Order::make.name}.${Asset::type.name}.${NftAssetType::token.name}", Sort.Direction.ASC)
        .on("${Order::take.name}.${Asset::type.name}.${NftAssetType::token.name}", Sort.Direction.ASC)
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

    // Orders for item which are not cancelled (for deactivation by suspicious items)
    val SELL_ORDERS_BY_ITEM_PLATFORM_NOT_CANCELLED = Index()
        .on(Order::platform.name, Sort.Direction.ASC)
        .on(Order::cancelled.name, Sort.Direction.ASC)
        .on("${Order::make.name}.${Asset::type.name}.${AssetType::nft.name}", Sort.Direction.ASC)
        .on("${Order::make.name}.${Asset::type.name}.${NftAssetType::token.name}", Sort.Direction.ASC)
        .on("${Order::make.name}.${Asset::type.name}.${NftAssetType::tokenId.name}", Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .partial(
            PartialIndexFilter.of(
                Criteria.where(Order::cancelled.name).isEqualTo(false)
                    .and("${Order::make.name}.${Asset::type.name}.${AssetType::nft.name}").isEqualTo(true)
            )
        )
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

    // --------------------- getAllOrders ---------------------//
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

    // --------------------- for updating status by start/end ---------------------//
    val BY_STATUS_AND_END_START = Index()
        .on(Order::status.name, Sort.Direction.ASC)
        .on(Order::end.name, Sort.Direction.ASC)
        .on(Order::start.name, Sort.Direction.ASC)
        .background()

    // --------------------- for updating status by start/end ---------------------//

    val BY_PLATFORM_MAKER_GLOBAL_COUNTER_STATUS: Index = Index()
        .on(Order::platform.name, Sort.Direction.ASC)
        .on(Order::maker.name, Sort.Direction.ASC)
        .on(MongoOrderRepository.COUNTER_HEX_KEY, Sort.Direction.ASC)
        .on(Order::status.name, Sort.Direction.ASC)
        .partial(PartialIndexFilter.of(Criteria(MongoOrderRepository.COUNTER_HEX_KEY).exists(true)))
        .background()

    val BY_PLATFORM_MAKER_ORDER_COUNTER_STATUS: Index = Index()
        .on(Order::platform.name, Sort.Direction.ASC)
        .on(Order::maker.name, Sort.Direction.ASC)
        .on("${Order::data.name}.${OrderLooksrareDataV2::orderNonce.name}", Sort.Direction.ASC)
        .partial(PartialIndexFilter.of(Criteria("${Order::data.name}.${OrderLooksrareDataV2::orderNonce.name}").exists(true)))
        .background()

    val BY_PLATFORM_MAKER_SUBSET_COUNTER_STATUS: Index = Index()
        .on(Order::platform.name, Sort.Direction.ASC)
        .on(Order::maker.name, Sort.Direction.ASC)
        .on("${Order::data.name}.${OrderLooksrareDataV2::subsetNonce.name}", Sort.Direction.ASC)
        .partial(PartialIndexFilter.of(Criteria("${Order::data.name}.${OrderLooksrareDataV2::subsetNonce.name}").exists(true)))
        .background()

    // --------------------- Other ---------------------//

    val BY_BID_PLATFORM_STATUS_LAST_UPDATED_AT = Index()
        .on("${Order::take.name}.${Asset::type.name}.${AssetType::nft.name}", Sort.Direction.ASC)
        .on(Order::platform.name, Sort.Direction.ASC)
        .on(Order::status.name, Sort.Direction.ASC)
        .on(Order::lastUpdateAt.name, Sort.Direction.ASC)
        .partial(
            PartialIndexFilter.of(
                Criteria.where("${Order::take.name}.${Asset::type.name}.${AssetType::nft.name}").isEqualTo(true)
                    .and(Order::platform.name).isEqualTo(Platform.RARIBLE)
            )
        )
        .background()

    // This index is used for activate/inactivate sale orders after changing approval
    val BY_MAKER_AND_STATUS_ONLY_SALE_ORDERS = Index()
        .on(Order::maker.name, Sort.Direction.ASC)
        .on(Order::status.name, Sort.Direction.ASC)
        .partial(PartialIndexFilter.of(Order::make / Asset::type / AssetType::nft isEqualTo true))
        .background()

    // TODO PT-1746 already exists in prod, but hasn't been declared in the code. Originally, this index is NOT correct
    // Its better to replace it with default-named index and include only token field in the index
    // since nft/status already "filtered" by sparse filter configuration
    //
    // name=idx_floor_price,
    // key=Document{{status=1.0, make.type.nft=1.0, make.type.token=1.0}},
    // partialFilterExpression=Document{{status=ACTIVE, make.type.nft=true}}}})]) // ADD
    val FLOOR_PRICE = Index()
        .named("idx_floor_price")
        .on(Order::status.name, Sort.Direction.ASC)
        .on("${Order::make.name}.${Asset::type.name}.${AssetType::nft.name}", Sort.Direction.ASC)
        .on("${Order::make.name}.${Asset::type.name}.${NftAssetType::token.name}", Sort.Direction.ASC)
        .partial(
            PartialIndexFilter.of(
                Criteria.where(Order::status.name).isEqualTo(OrderStatus.ACTIVE)
                    .and("${Order::take.name}.${Asset::type.name}.${AssetType::nft.name}").isEqualTo(true)
                    .and(Order::status.name).isEqualTo(OrderStatus.ACTIVE)
            )
        )

    // --------------------- for searching by hash ---------------------//
    val BY_HASH = Index()
        .on(Order::hash.name, Sort.Direction.ASC)
        .background()

    val ALL_INDEXES = listOf(
        SELL_ORDERS_PLATFORM_DEFINITION,
        SELL_ORDERS_STATUS_DEFINITION,
        SELL_ORDERS_PLATFORM_STATUS_DEFINITION,

        SELL_ORDERS_BY_ITEM_SORT_BY_USD_PRICE_DEFINITION,
        SELL_ORDERS_BY_ITEM_SORT_BY_PRICE_DEFINITION,
        SELL_ORDERS_BY_ITEM_PLATFORM_SORT_BY_USD_PRICE_DEFINITION,
        SELL_ORDERS_BY_ITEM_CURRENCY_STATUS_SORT_BY_PRICE_DEFINITION,
        SELL_ORDERS_BY_ITEM_MAKER_SORT_BY_PRICE_DEFINITION,
        SELL_ORDERS_BY_ITEM_PLATFORM_NOT_CANCELLED,
        SELL_ORDERS_BY_COLLECTION_CURRENCY_SORT_BY_PRICE_DEFINITION,

        SELL_ORDERS_BY_COLLECTION_DEFINITION,
        SELL_ORDERS_BY_COLLECTION_PLATFORM_DEFINITION,

        SELL_ORDERS_BY_MAKER_DEFINITION,
        SELL_ORDERS_BY_MAKER_PLATFORM_STATUS_DEFINITION,

        BIDS_BY_ITEM_DEFINITION_DEPRECATED,
        BIDS_BY_ITEM_DEFINITION,

        BY_LAST_UPDATE_AND_ID_DEFINITION,
        BY_DB_UPDATE_UPDATE_DEFINITION,
        BY_LAST_UPDATE_AND_STATUS_AND_ID_DEFINITION,

        BY_STATUS_AND_END_START,
        BY_PLATFORM_MAKER_GLOBAL_COUNTER_STATUS,
        BY_PLATFORM_MAKER_ORDER_COUNTER_STATUS,
        BY_PLATFORM_MAKER_SUBSET_COUNTER_STATUS,

        BY_BID_PLATFORM_STATUS_LAST_UPDATED_AT,
        BY_MAKER_AND_STATUS_ONLY_SALE_ORDERS,
        SELL_ORDERS_BY_CURRENCY_COLLECTION_DEFINITION,

        BY_HASH
    )
}
