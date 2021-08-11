package com.rarible.protocol.nft.api.converter

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.*
import com.rarible.protocol.nft.api.configuration.NftIndexerApiProperties
import com.rarible.protocol.nft.core.repository.history.ActivityItemHistoryFilter
import com.rarible.protocol.nft.core.repository.history.CollectionActivityItemHistoryFilter
import com.rarible.protocol.nft.core.repository.history.ItemActivityItemHistoryFilter
import com.rarible.protocol.nft.core.repository.history.UserActivityItemHistoryFilter
import org.springframework.stereotype.Component

@Component
class ActivityHistoryFilterConverter(properties: NftIndexerApiProperties) {
    private val skipHeavyRequest = properties.skipHeavyRequest

    fun convert(source: NftActivityFilterDto, activityContinuation: ActivityContinuationDto?): List<ActivityItemHistoryFilter> {
        val continuation = activityContinuation?.let { ContinuationConverter.convert(it) }

        return when (source) {
            is NftActivityFilterAllDto -> source.types.map {
                when (it) {
                    NftActivityFilterAllDto.Types.TRANSFER -> ActivityItemHistoryFilter.AllTransfer(continuation)
                    NftActivityFilterAllDto.Types.MINT -> ActivityItemHistoryFilter.AllMint(continuation)
                    NftActivityFilterAllDto.Types.BURN -> ActivityItemHistoryFilter.AllBurn(continuation)
                }
            }
            is NftActivityFilterByUserDto -> source.types.map {
                val users = if (source.users.size > 1 && skipHeavyRequest) listOf(source.users.first()) else source.users
                when (it) {
                    NftActivityFilterByUserDto.Types.TRANSFER_FROM -> UserActivityItemHistoryFilter.ByUserTransferFrom(
                        users,
                        continuation
                    )
                    NftActivityFilterByUserDto.Types.TRANSFER_TO -> UserActivityItemHistoryFilter.ByUserTransferTo(
                        users,
                        continuation
                    )
                    NftActivityFilterByUserDto.Types.MINT -> UserActivityItemHistoryFilter.ByUserMint(
                        users,
                        continuation
                    )
                    NftActivityFilterByUserDto.Types.BURN -> UserActivityItemHistoryFilter.ByUserBurn(
                        users,
                        continuation
                    )
                }
            }
            is NftActivityFilterByItemDto -> source.types.map {
                val contract = source.contract
                val tokenId = EthUInt256.of(source.tokenId)
                when (it) {
                    NftActivityFilterByItemDto.Types.TRANSFER -> ItemActivityItemHistoryFilter.ByItemTransfer(
                        contract,
                        tokenId,
                        continuation
                    )
                    NftActivityFilterByItemDto.Types.MINT -> ItemActivityItemHistoryFilter.ByItemMint(
                        contract,
                        tokenId,
                        continuation
                    )
                    NftActivityFilterByItemDto.Types.BURN -> ItemActivityItemHistoryFilter.ByItemBurn(
                        contract,
                        tokenId,
                        continuation
                    )
                }
            }
            is NftActivityFilterByCollectionDto -> source.types.map {
                val contract = source.contract
                when (it) {
                    NftActivityFilterByCollectionDto.Types.TRANSFER -> CollectionActivityItemHistoryFilter.ByCollectionTransfer(
                        contract,
                        continuation
                    )
                    NftActivityFilterByCollectionDto.Types.MINT -> CollectionActivityItemHistoryFilter.ByCollectionMint(
                        contract,
                        continuation
                    )
                    NftActivityFilterByCollectionDto.Types.BURN -> CollectionActivityItemHistoryFilter.ByCollectionBurn(
                        contract,
                        continuation
                    )
                }
            }
        }
    }
}
