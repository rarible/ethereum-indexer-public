package com.rarible.protocol.order.core.repository.order

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.*
import com.rarible.protocol.dto.Continuation
import com.rarible.protocol.order.core.converters.model.PlatformConverter
import com.rarible.protocol.order.core.misc.div
import com.rarible.protocol.order.core.misc.limit
import com.rarible.protocol.order.core.model.*
import org.bson.Document
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.*
import scalether.domain.Address

object OrderFilterCriteria {
    fun OrderFilterDto.toCriteria(continuation: String?, limit: Int?): Query {
        //for sell filters we sort orders by make price ASC
        //for bid filters we sort orders by take price DESC
        val (criteria, hint) = when (this) {
            is OrderFilterAllDto -> Criteria() withHint OrderRepositoryIndexes.BY_LAST_UPDATE_AND_ID_DEFINITION.indexKeys
            is OrderFilterSellDto -> sell().withNoHint()
            is OrderFilterSellByItemDto -> sellByItem(contract, EthUInt256(tokenId), maker).withNoHint()
            is OrderFilterSellByCollectionDto -> sellByCollection(collection).withNoHint()
            is OrderFilterSellByMakerDto -> sellByMaker(maker).withNoHint()
            is OrderFilterBidByItemDto -> bidByItem(contract, EthUInt256(tokenId), maker).withNoHint()
            is OrderFilterBidByMakerDto -> bidByMaker(maker).withNoHint()
        }

        val requestLimit = limit.limit()

        val query = Query(
            criteria
                .forPlatform(convert(platform))
                .scrollTo(continuation, this.sort)
                .fromOrigin(origin)
        ).limit(requestLimit).with(sort(this.sort))

        if (hint != null) {
            query.withHint(hint)
        }
        this.status?.let {
            if (it.isNotEmpty()) {
                val statuses = it.map { OrderStatus.valueOf(it.name) }
                query.addCriteria(Order::status inValues statuses)
            }
        }
        return query
    }

    private fun sort(sort: OrderFilterDto.Sort): Sort {
        return when (sort) {
            OrderFilterDto.Sort.LAST_UPDATE_DESC -> Sort.by(
                Sort.Direction.DESC,
                Order::lastUpdateAt.name,
                Order::hash.name
            )
            OrderFilterDto.Sort.LAST_UPDATE_ASC -> Sort.by(
                Sort.Direction.ASC,
                Order::lastUpdateAt.name,
                Order::hash.name
            )
            OrderFilterDto.Sort.MAKE_PRICE_ASC -> Sort.by(
                Sort.Direction.ASC,
                Order::makePriceUsd.name,
                Order::hash.name
            )
            OrderFilterDto.Sort.TAKE_PRICE_DESC -> Sort.by(
                Sort.Direction.DESC,
                Order::takePriceUsd.name,
                Order::hash.name
            )
        }
    }

    private fun sellByMaker(maker: Address) =
        sell().and(Order::maker.name).isEqualTo(maker)

    private fun sellByCollection(collection: Address) =
        sell().and("${Order::make.name}.${Asset::type.name}.${NftAssetType::token.name}").isEqualTo(collection)

    private fun sellByItem(token: Address, tokenId: EthUInt256, maker: Address?) = run {
        val c = (Order::make / Asset::type / NftAssetType::token isEqualTo token)
            .and(Order::make / Asset::type / NftAssetType::tokenId).isEqualTo(tokenId)

        if (maker != null) {
            c.and(Order::maker.name).`is`(maker)
        } else {
            c
        }
    }

    private fun sell() =
        Criteria.where("${Order::make.name}.${Asset::type.name}.${AssetType::nft.name}").isEqualTo(true)

    private fun bidByItem(token: Address, tokenId: EthUInt256, maker: Address?) = run {
        val criteria = Criteria
            .where("${Order::take.name}.${Asset::type.name}.${NftAssetType::token.name}").isEqualTo(token)
            .and("${Order::take.name}.${Asset::type.name}.${NftAssetType::tokenId.name}").isEqualTo(tokenId)

        if (maker != null) {
            criteria.and(Order::maker.name).isEqualTo(maker)
        } else {
            criteria
        }
    }

    private fun bidByMaker(maker: Address) =
        Criteria()
            .and(Order::maker.name).isEqualTo(maker)
            .and("${Order::take.name}.${Asset::type.name}.${AssetType::nft.name}").isEqualTo(true)

    private infix fun Criteria.fromOrigin(origin: Address?) = origin?.let {
        and("${Order::data.name}.${OrderRaribleV2DataV1::originFees.name}")
            .elemMatch(Criteria.where(Part::account.name).`is`(origin))
    } ?: this

    private infix fun Criteria.forPlatform(platform: Platform?) = platform?.let {
        and(Order::platform).isEqualTo(platform)
    } ?: this

    private fun Criteria.scrollTo(continuation: String?, sort: OrderFilterDto.Sort) =
        when (sort) {
            OrderFilterDto.Sort.LAST_UPDATE_DESC -> {
                val lastDate = Continuation.parse<Continuation.LastDate>(continuation)
                lastDate?.let { c ->
                    this.orOperator(
                        Order::lastUpdateAt lt c.afterDate,
                        Criteria().andOperator(
                            Order::lastUpdateAt isEqualTo c.afterDate,
                            Order::hash lt c.afterId
                        )
                    )
                } ?: this
            }
            OrderFilterDto.Sort.LAST_UPDATE_ASC -> {
                val lastDate = Continuation.parse<Continuation.LastDate>(continuation)
                lastDate?.let { c ->
                    this.orOperator(
                        Order::lastUpdateAt gt c.afterDate,
                        Criteria().andOperator(
                            Order::lastUpdateAt isEqualTo c.afterDate,
                            Order::hash gt c.afterId
                        )
                    )
                } ?: this
            }
            OrderFilterDto.Sort.TAKE_PRICE_DESC -> {
                val price = Continuation.parse<Continuation.Price>(continuation)
                price?.let { c ->
                    this.orOperator(
                        Order::takePriceUsd lt c.afterPrice,
                        Criteria().andOperator(
                            Order::takePriceUsd isEqualTo c.afterPrice,
                            Order::hash lt c.afterId
                        )
                    )
                } ?: this
            }
            OrderFilterDto.Sort.MAKE_PRICE_ASC -> {
                val price = Continuation.parse<Continuation.Price>(continuation)
                price?.let { c ->
                    this.orOperator(
                        Order::makePriceUsd gt c.afterPrice,
                        Criteria().andOperator(
                            Order::makePriceUsd isEqualTo c.afterPrice,
                            Order::hash gt c.afterId
                        )
                    )
                } ?: this
            }
        }

    private fun convert(platform: PlatformDto?): Platform? = PlatformConverter.convert(platform)

    private infix fun Criteria.withHint(index: Document) = Pair(this, index)
    private fun Criteria.withNoHint() = Pair<Criteria, Document?>(this, null)
}
