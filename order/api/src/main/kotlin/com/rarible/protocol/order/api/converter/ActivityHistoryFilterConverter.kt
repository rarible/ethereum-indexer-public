package com.rarible.protocol.order.api.converter

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.*
import com.rarible.protocol.dto.OrderActivityFilterAllDto.Types.*
import com.rarible.protocol.order.api.configuration.OrderIndexerApiProperties
import com.rarible.protocol.order.core.repository.exchange.ActivityExchangeHistoryFilter
import com.rarible.protocol.order.core.repository.exchange.CollectionActivityExchangeHistoryFilter
import com.rarible.protocol.order.core.repository.exchange.ItemActivityExchangeHistoryFilter
import com.rarible.protocol.order.core.repository.exchange.UserActivityExchangeHistoryFilter
import com.rarible.protocol.order.core.model.ActivitySort
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class ActivityHistoryFilterConverter(properties: OrderIndexerApiProperties) {

    private val skipHeavyRequest = properties.skipHeavyRequest

    fun convert(
        source: OrderActivityFilterDto,
        sort: ActivitySort,
        activityContinuation: ActivityContinuationDto?
    ): List<ActivityExchangeHistoryFilter> {
        val continuation = activityContinuation?.let { ContinuationConverter.convert(it) }

        return when (source) {
            is OrderActivityFilterAllDto -> source.types.flatMap {
                when (it) {
                    MATCH -> listOf(
                        ActivityExchangeHistoryFilter.AllSell(sort, continuation)
                    )
                    CANCEL_BID -> listOf(
                        ActivityExchangeHistoryFilter.AllCanceledBid(sort, continuation)
                    )
                    CANCEL_LIST -> listOf(
                        ActivityExchangeHistoryFilter.AllCanceledSell(sort, continuation)
                    )
                    LIST, BID -> emptyList()
                }
            }
            is OrderActivityFilterByUserDto -> {
                val users = if (source.users.size > 1 && skipHeavyRequest) listOf(source.users.first()) else source.users
                val from = source.from?.let { from -> Instant.ofEpochSecond(from) }
                val to = source.to?.let { to -> Instant.ofEpochSecond(to) }
                source.types.flatMap {
                    when (it) {
                        OrderActivityFilterByUserDto.Types.SELL -> listOf(
                            UserActivityExchangeHistoryFilter.ByUserSell(sort, users, from, to, continuation)
                        )
                        OrderActivityFilterByUserDto.Types.BUY -> listOf(
                            UserActivityExchangeHistoryFilter.ByUserBuy(sort, users, from, to, continuation)
                        )
                        OrderActivityFilterByUserDto.Types.CANCEL_BID -> listOf(
                            UserActivityExchangeHistoryFilter.ByUserCanceledBid(sort, users, from, to, continuation)
                        )
                        OrderActivityFilterByUserDto.Types.CANCEL_LIST -> listOf(
                            UserActivityExchangeHistoryFilter.ByUserCanceledSell(sort, users, from, to, continuation)
                        )
                        OrderActivityFilterByUserDto.Types.MAKE_BID,
                        OrderActivityFilterByUserDto.Types.LIST,
                        OrderActivityFilterByUserDto.Types.GET_BID -> emptyList()
                    }
                }
            }
            is OrderActivityFilterByItemDto -> source.types.flatMap {
                when (it) {
                    OrderActivityFilterByItemDto.Types.MATCH -> listOf(
                        ItemActivityExchangeHistoryFilter.ByItemSell(sort, source.contract, EthUInt256.of(source.tokenId), continuation)
                    )
                    OrderActivityFilterByItemDto.Types.CANCEL_BID -> listOf(
                        ItemActivityExchangeHistoryFilter.ByItemCanceledBid(sort, source.contract, EthUInt256.of(source.tokenId), continuation)
                    )
                    OrderActivityFilterByItemDto.Types.CANCEL_LIST -> listOf(
                        ItemActivityExchangeHistoryFilter.ByItemCanceledSell(sort, source.contract, EthUInt256.of(source.tokenId), continuation)
                    )
                    OrderActivityFilterByItemDto.Types.BID,
                    OrderActivityFilterByItemDto.Types.LIST -> emptyList()
                }
            }
            is OrderActivityFilterByCollectionDto -> source.types.flatMap {
                when (it) {
                    OrderActivityFilterByCollectionDto.Types.MATCH -> listOf(
                        CollectionActivityExchangeHistoryFilter.ByCollectionSell(sort, source.contract, continuation)
                    )
                    OrderActivityFilterByCollectionDto.Types.CANCEL_BID -> listOf(
                        CollectionActivityExchangeHistoryFilter.ByCollectionCanceledBid(sort, source.contract, continuation)
                    )
                    OrderActivityFilterByCollectionDto.Types.CANCEL_LIST -> listOf(
                        CollectionActivityExchangeHistoryFilter.ByCollectionCanceledSell(sort, source.contract, continuation)
                    )
                    OrderActivityFilterByCollectionDto.Types.BID,
                    OrderActivityFilterByCollectionDto.Types.LIST -> emptyList()
                }
            }
        }
    }

    fun syncConvert(
        sort: ActivitySort,
        filter: List<OrderActivitiesSyncTypesDto>?,
        activityContinuation: ActivityContinuationDto?
    ): List<ActivityExchangeHistoryFilter> {
        val continuation = activityContinuation?.let { ContinuationConverter.convert(it) }

        if (filter == null) {
            return listOf(ActivityExchangeHistoryFilter.AllSync(sort, continuation))
        }

        val filterSet = filter.toSet()
        val count = filterSet.count { it == OrderActivitiesSyncTypesDto.BID || it == OrderActivitiesSyncTypesDto.LIST }

        if (filterSet.size == count) {
            return listOf(ActivityExchangeHistoryFilter.AllSync(sort, continuation))
        }

        return filterSet.mapNotNull {
            when (it) {
                OrderActivitiesSyncTypesDto.MATCH -> ActivityExchangeHistoryFilter.AllSell(sort, continuation)
                OrderActivitiesSyncTypesDto.CANCEL_BID ->  ActivityExchangeHistoryFilter.AllCanceledBid(sort, continuation)
                OrderActivitiesSyncTypesDto.CANCEL_LIST -> ActivityExchangeHistoryFilter.AllCanceledSell(sort, continuation)
                OrderActivitiesSyncTypesDto.LIST, OrderActivitiesSyncTypesDto.BID -> null
            }
        }
    }
}
