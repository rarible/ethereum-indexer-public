package com.rarible.protocol.order.core.repository.order

import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.AssetType
import com.rarible.protocol.order.core.model.LogEventKey
import com.rarible.protocol.order.core.model.NftAssetType
import com.rarible.protocol.order.core.model.OrderVersion
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.index.Index

internal object OrderVersionRepositoryIndexes {
    val ALL_BID_DEFINITION: Index = Index()
        .on("${OrderVersion::take.name}.${Asset::type.name}.${AssetType::nft.name}", Sort.Direction.ASC)
        .on(OrderVersion::createdAt.name, Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    val ALL_LIST_DEFINITION: Index = Index()
        .on("${OrderVersion::make.name}.${Asset::type.name}.${AssetType::nft.name}", Sort.Direction.ASC)
        .on(OrderVersion::createdAt.name, Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    val MAKER_BID_DEFINITION: Index = Index()
        .on("${OrderVersion::take.name}.${Asset::type.name}.${AssetType::nft.name}", Sort.Direction.ASC)
        .on(OrderVersion::maker.name, Sort.Direction.ASC)
        .on(OrderVersion::createdAt.name, Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    val MAKER_LIST_DEFINITION: Index = Index()
        .on("${OrderVersion::make.name}.${Asset::type.name}.${AssetType::nft.name}", Sort.Direction.ASC)
        .on(OrderVersion::maker.name, Sort.Direction.ASC)
        .on(OrderVersion::createdAt.name, Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    val COLLECTION_BID_DEFINITION: Index = Index()
        .on("${OrderVersion::take.name}.${Asset::type.name}.${NftAssetType::nft.name}", Sort.Direction.ASC)
        .on("${OrderVersion::take.name}.${Asset::type.name}.${NftAssetType::token.name}", Sort.Direction.ASC)
        .on(OrderVersion::createdAt.name, Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    val COLLECTION_LIST_DEFINITION: Index = Index()
        .on("${OrderVersion::make.name}.${Asset::type.name}.${NftAssetType::nft.name}", Sort.Direction.ASC)
        .on("${OrderVersion::make.name}.${Asset::type.name}.${NftAssetType::token.name}", Sort.Direction.ASC)
        .on(OrderVersion::createdAt.name, Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    val ITEM_LIST_DEFINITION: Index = Index()
        .on("${OrderVersion::make.name}.${Asset::type.name}.${NftAssetType::token.name}", Sort.Direction.ASC)
        .on("${OrderVersion::make.name}.${Asset::type.name}.${NftAssetType::tokenId.name}", Sort.Direction.ASC)
        .on(OrderVersion::createdAt.name, Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    val ITEM_BID_DEFINITION: Index = Index()
        .on("${OrderVersion::take.name}.${Asset::type.name}.${NftAssetType::token.name}", Sort.Direction.ASC)
        .on("${OrderVersion::take.name}.${Asset::type.name}.${NftAssetType::tokenId.name}", Sort.Direction.ASC)
        .on(OrderVersion::createdAt.name, Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    private val ITEM_TAKE_PRICE_USD_BID_DEFINITION: Index = Index()
        .on("${OrderVersion::take.name}.${Asset::type.name}.${NftAssetType::token.name}", Sort.Direction.ASC)
        .on("${OrderVersion::take.name}.${Asset::type.name}.${NftAssetType::tokenId.name}", Sort.Direction.ASC)
        .on(OrderVersion::takePriceUsd.name, Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    private val ITEM_TAKE_PRICE_BID_DEFINITION: Index = Index()
        .on("${OrderVersion::take.name}.${Asset::type.name}.${NftAssetType::token.name}", Sort.Direction.ASC)
        .on("${OrderVersion::take.name}.${Asset::type.name}.${NftAssetType::tokenId.name}", Sort.Direction.ASC)
        .on("${OrderVersion::make.name}.${Asset::type.name}.${NftAssetType::token.name}", Sort.Direction.ASC)
        .on(OrderVersion::takePrice.name, Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    val HASH_AND_ID_DEFINITION: Index = Index()
        .on(OrderVersion::hash.name, Sort.Direction.ASC)
        .on("_id", Sort.Direction.ASC)
        .background()

    val ON_CHAIN_ORDER_PRIMARY_KEY_DEFINITION: Index = Index()
        .on("${OrderVersion::onChainOrderKey.name}.${LogEventKey::databaseKey.name}", Sort.Direction.ASC)
        .sparse()
        .unique()
        .background()

    val ALL_INDEXES = listOf(
        ALL_BID_DEFINITION,
        ALL_LIST_DEFINITION,
        MAKER_BID_DEFINITION,
        MAKER_LIST_DEFINITION,
        COLLECTION_BID_DEFINITION,
        COLLECTION_LIST_DEFINITION,
        ITEM_LIST_DEFINITION,
        ITEM_BID_DEFINITION,
        ITEM_TAKE_PRICE_USD_BID_DEFINITION,
        ITEM_TAKE_PRICE_BID_DEFINITION,
        HASH_AND_ID_DEFINITION,
        ON_CHAIN_ORDER_PRIMARY_KEY_DEFINITION
    )
}
