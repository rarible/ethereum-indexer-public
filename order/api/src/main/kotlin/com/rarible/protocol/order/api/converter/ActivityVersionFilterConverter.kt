package com.rarible.protocol.order.api.converter

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.*
import com.rarible.protocol.order.api.service.nft.NftAssetService
import com.rarible.protocol.order.core.repository.order.ActivityOrderVersionFilter
import com.rarible.protocol.order.core.repository.order.CollectionActivityOrderVersionFilter
import com.rarible.protocol.order.core.repository.order.ItemActivityOrderVersionFilter
import com.rarible.protocol.order.core.repository.order.UserActivityOrderVersionFilter
import com.rarible.protocol.order.core.model.ActivitySort
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ActivityVersionFilterConverter(
    private val nftAssetService: NftAssetService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun convert(
        source: OrderActivityFilterDto,
        sort: ActivitySort,
        activityContinuation: ActivityContinuationDto?
    ): List<ActivityOrderVersionFilter> = coroutineScope {
        val continuation = activityContinuation?.let { ContinuationConverter.convert(it) }

        when (source) {
            is OrderActivityFilterAllDto -> source.types.mapNotNull {
                when (it) {
                    OrderActivityFilterAllDto.Types.LIST -> ActivityOrderVersionFilter.AllList(sort, continuation)
                    OrderActivityFilterAllDto.Types.BID -> ActivityOrderVersionFilter.AllBid(sort, continuation)
                    OrderActivityFilterAllDto.Types.MATCH -> null
                }
            }
            is OrderActivityFilterByUserDto -> source.types.flatMap {
                when (it) {
                    OrderActivityFilterByUserDto.Types.LIST -> listOf(
                        UserActivityOrderVersionFilter.ByUserList(sort, source.users, continuation)
                    )
                    OrderActivityFilterByUserDto.Types.MAKE_BID -> listOf(
                        UserActivityOrderVersionFilter.ByUserMakeBid(sort, source.users, continuation)
                    )
                    OrderActivityFilterByUserDto.Types.GET_BID -> {
                        logger.info("Get bids for ${source.users.size}} users")

                        source.users
                            .map { user -> async { nftAssetService.getOwnerNftAssets(user) } }
                            .awaitAll()
                            .flatten()
                            .distinct()
                            .map { item ->
                                ItemActivityOrderVersionFilter.ByItemBid(
                                    sort,
                                    item.contract,
                                    item.tokenId,
                                    continuation
                                )
                            }
                    }
                    OrderActivityFilterByUserDto.Types.SELL,
                    OrderActivityFilterByUserDto.Types.BUY -> emptyList()
                }
            }
            is OrderActivityFilterByItemDto -> source.types.mapNotNull {
                when (it) {
                    OrderActivityFilterByItemDto.Types.LIST -> ItemActivityOrderVersionFilter.ByItemList(
                        sort,
                        source.contract,
                        EthUInt256.of(source.tokenId),
                        continuation
                    )
                    OrderActivityFilterByItemDto.Types.BID -> ItemActivityOrderVersionFilter.ByItemBid(
                        sort,
                        source.contract,
                        EthUInt256.of(source.tokenId),
                        continuation
                    )
                    OrderActivityFilterByItemDto.Types.MATCH -> null
                }
            }
            is OrderActivityFilterByCollectionDto -> source.types.mapNotNull {
                when (it) {
                    OrderActivityFilterByCollectionDto.Types.LIST -> CollectionActivityOrderVersionFilter.ByCollectionList(
                        sort,
                        source.contract,
                        continuation
                    )
                    OrderActivityFilterByCollectionDto.Types.BID -> CollectionActivityOrderVersionFilter.ByCollectionBid(
                        sort,
                        source.contract,
                        continuation
                    )
                    OrderActivityFilterByCollectionDto.Types.MATCH -> null
                }
            }
        }
    }
}
