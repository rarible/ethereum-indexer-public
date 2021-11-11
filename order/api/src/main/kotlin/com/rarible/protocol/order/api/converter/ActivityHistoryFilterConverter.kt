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
                    BID -> listOf(
                        //TODO: Remove after market move to CANCEL_BID/CANCEL_LIST
                        ActivityExchangeHistoryFilter.AllCanceledBid(sort, continuation)
                    )
                    CANCEL_BID -> listOf(
                        ActivityExchangeHistoryFilter.AllCanceledBid(sort, continuation)
                    )
                    CANCEL_LIST -> listOf(
                        ActivityExchangeHistoryFilter.AllCanceledSell(sort, continuation)
                    )
                    LIST -> emptyList()
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
                        OrderActivityFilterByUserDto.Types.MAKE_BID -> listOf(
                            //TODO: Remove after market move to CANCEL_BID/CANCEL_LIST
                            UserActivityExchangeHistoryFilter.ByUserCanceledBid(sort, users, from, to, continuation)
                        )
                        OrderActivityFilterByUserDto.Types.CANCEL_BID -> listOf(
                            UserActivityExchangeHistoryFilter.ByUserCanceledBid(sort, users, from, to, continuation)
                        )
                        OrderActivityFilterByUserDto.Types.CANCEL_LIST -> listOf(
                            UserActivityExchangeHistoryFilter.ByUserCanceledSell(sort, users, from, to, continuation)
                        )
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
                    OrderActivityFilterByItemDto.Types.BID -> listOf(
                        //TODO: Remove after market move to CANCEL_BID/CANCEL_LIST
                        ItemActivityExchangeHistoryFilter.ByItemCanceledBid(sort, source.contract, EthUInt256.of(source.tokenId), continuation)
                    )
                    OrderActivityFilterByItemDto.Types.CANCEL_BID -> listOf(
                        ItemActivityExchangeHistoryFilter.ByItemCanceledBid(sort, source.contract, EthUInt256.of(source.tokenId), continuation)
                    )
                    OrderActivityFilterByItemDto.Types.CANCEL_LIST -> listOf(
                        ItemActivityExchangeHistoryFilter.ByItemCanceledSell(sort, source.contract, EthUInt256.of(source.tokenId), continuation)
                    )
                    OrderActivityFilterByItemDto.Types.LIST -> emptyList()
                }
            }
            is OrderActivityFilterByCollectionDto -> source.types.flatMap {
                when (it) {
                    OrderActivityFilterByCollectionDto.Types.MATCH -> listOf(
                        CollectionActivityExchangeHistoryFilter.ByCollectionSell(sort, source.contract, continuation)
                    )
                    OrderActivityFilterByCollectionDto.Types.BID -> listOf(
                        //TODO: Remove after market move to CANCEL_BID/CANCEL_LIST
                        CollectionActivityExchangeHistoryFilter.ByCollectionCanceledBid(sort, source.contract, continuation)
                    )
                    OrderActivityFilterByCollectionDto.Types.CANCEL_BID -> listOf(
                        CollectionActivityExchangeHistoryFilter.ByCollectionCanceledBid(sort, source.contract, continuation)
                    )
                    OrderActivityFilterByCollectionDto.Types.CANCEL_LIST -> listOf(
                        CollectionActivityExchangeHistoryFilter.ByCollectionCanceledSell(sort, source.contract, continuation)
                    )
                    OrderActivityFilterByCollectionDto.Types.LIST -> emptyList()
                }
            }
        }
    }
}
