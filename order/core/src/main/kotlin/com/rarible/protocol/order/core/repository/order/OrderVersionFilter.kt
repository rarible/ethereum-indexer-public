package com.rarible.protocol.order.core.repository.order

import com.rarible.protocol.order.core.misc.div
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.AssetType
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.OrderVersion
import org.bson.Document
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.Criteria

abstract class OrderVersionFilter {
    internal abstract fun getCriteria(): Criteria

    internal open val hint: Document? = null
    internal abstract val sort: Sort
    open val limit: Int? = null

    protected companion object {
        val makeNftKey = OrderVersion::make / Asset::type / AssetType::nft
        val takeNftKey = OrderVersion::take / Asset::type / AssetType::nft

        val takeNftContractKey = OrderVersion::take / Asset::type / Erc721AssetType::token
        val takeNftTokenIdKey = OrderVersion::take / Asset::type / Erc721AssetType::tokenId

        val makeNftContractKey = OrderVersion::make / Asset::type / Erc721AssetType::token
        val makeNftTokenIdKey = OrderVersion::make / Asset::type / Erc721AssetType::tokenId
    }
}
