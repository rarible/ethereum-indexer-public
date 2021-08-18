package com.rarible.protocol.order.api.converter

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.*
import com.rarible.protocol.order.api.configuration.OrderIndexerApiProperties
import com.rarible.protocol.order.core.repository.exchange.ActivityExchangeHistoryFilter
import com.rarible.protocol.order.core.repository.exchange.CollectionActivityExchangeHistoryFilter
import com.rarible.protocol.order.core.repository.exchange.ItemActivityExchangeHistoryFilter
import com.rarible.protocol.order.core.repository.exchange.UserActivityExchangeHistoryFilter
import org.springframework.stereotype.Component


@Component
class ActivityHistoryFilterConverter(properties: OrderIndexerApiProperties) {

    private val skipHeavyRequest = properties.skipHeavyRequest

    fun convert(
        source: OrderActivityFilterDto,
        activityContinuation: ActivityContinuationDto?
    ): List<ActivityExchangeHistoryFilter> {
        val continuation = activityContinuation?.let { ContinuationConverter.convert(it) }

        return when (source) {
            is OrderActivityFilterAllDto -> source.types.flatMap {
                when (it) {
                    OrderActivityFilterAllDto.Types.MATCH -> listOf(
                        ActivityExchangeHistoryFilter.AllSell(continuation)
                    )
                    OrderActivityFilterAllDto.Types.BID -> listOf(
                        ActivityExchangeHistoryFilter.AllCanceledBid(continuation)
                    )
                    OrderActivityFilterAllDto.Types.LIST -> emptyList()
                }
            }
            is OrderActivityFilterByUserDto -> source.types.flatMap {
                val users =
                    if (source.users.size > 1 && skipHeavyRequest) listOf(source.users.first()) else source.users
                when (it) {
                    OrderActivityFilterByUserDto.Types.SELL -> listOf(
                        UserActivityExchangeHistoryFilter.ByUserSell(users, continuation)
                    )
                    OrderActivityFilterByUserDto.Types.BUY -> listOf(
                        UserActivityExchangeHistoryFilter.ByUserBuy(users, continuation)
                    )
                    OrderActivityFilterByUserDto.Types.MAKE_BID -> listOf(
                        UserActivityExchangeHistoryFilter.ByUserCanceledBid(users, continuation)
                    )
                    OrderActivityFilterByUserDto.Types.LIST,
                    OrderActivityFilterByUserDto.Types.GET_BID -> emptyList()
                }
            }
            is OrderActivityFilterByItemDto -> source.types.flatMap {
                when (it) {
                    OrderActivityFilterByItemDto.Types.MATCH -> listOf(
                        ItemActivityExchangeHistoryFilter.ByItemSell(source.contract, EthUInt256.of(source.tokenId), continuation)
                    )
                    OrderActivityFilterByItemDto.Types.BID -> listOf(
                        ItemActivityExchangeHistoryFilter.ByItemCanceledBid(source.contract, EthUInt256.of(source.tokenId), continuation)
                    )
                    OrderActivityFilterByItemDto.Types.LIST -> emptyList()
                }
            }
            is OrderActivityFilterByCollectionDto -> source.types.flatMap {
                when (it) {
                    OrderActivityFilterByCollectionDto.Types.MATCH -> listOf(
                        CollectionActivityExchangeHistoryFilter.ByCollectionSell(source.contract, continuation)
                    )
                    OrderActivityFilterByCollectionDto.Types.BID -> listOf(
                        CollectionActivityExchangeHistoryFilter.ByCollectionCanceledBid(source.contract, continuation)
                    )
                    OrderActivityFilterByCollectionDto.Types.LIST -> emptyList()
                }
            }
        }
    }
}
