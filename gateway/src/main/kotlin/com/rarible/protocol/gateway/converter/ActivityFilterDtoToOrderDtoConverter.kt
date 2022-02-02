package com.rarible.protocol.gateway.converter

import com.rarible.protocol.dto.*

object ActivityFilterDtoToOrderDtoConverter {
    fun convert(source: ActivityFilterDto): OrderActivityFilterDto? {
        return when (source) {
            is ActivityFilterAllDto -> {
                val orderTypes = source.types.mapNotNull { type ->
                    when (type) {
                        ActivityFilterAllTypeDto.BID -> OrderActivityFilterAllDto.Types.BID
                        ActivityFilterAllTypeDto.LIST -> OrderActivityFilterAllDto.Types.LIST
                        ActivityFilterAllTypeDto.SELL -> OrderActivityFilterAllDto.Types.MATCH
                        ActivityFilterAllTypeDto.TRANSFER,
                        ActivityFilterAllTypeDto.MINT,
                        ActivityFilterAllTypeDto.BURN -> null
                        ActivityFilterAllTypeDto.CANCEL_BID -> null
                        ActivityFilterAllTypeDto.CANCEL_LIST -> null
                    }
                }
                if (orderTypes.isNotEmpty()) OrderActivityFilterAllDto(orderTypes) else null
            }
            is ActivityFilterByCollectionDto -> {
                val orderTypes = source.types.mapNotNull { type ->
                    when (type) {
                        ActivityFilterByCollectionTypeDto.BID -> OrderActivityFilterByCollectionDto.Types.BID
                        ActivityFilterByCollectionTypeDto.LIST -> OrderActivityFilterByCollectionDto.Types.LIST
                        ActivityFilterByCollectionTypeDto.MATCH -> OrderActivityFilterByCollectionDto.Types.MATCH
                        ActivityFilterByCollectionTypeDto.TRANSFER,
                        ActivityFilterByCollectionTypeDto.MINT,
                        ActivityFilterByCollectionTypeDto.BURN -> null
                        ActivityFilterByCollectionTypeDto.CANCEL_BID -> null
                        ActivityFilterByCollectionTypeDto.CANCEL_LIST -> null
                    }
                }
                if (orderTypes.isNotEmpty()) OrderActivityFilterByCollectionDto(source.contract, orderTypes) else null
            }
            is ActivityFilterByItemDto -> {
                val orderTypes = source.types.mapNotNull { type ->
                    when (type) {
                        ActivityFilterByItemTypeDto.BID -> OrderActivityFilterByItemDto.Types.BID
                        ActivityFilterByItemTypeDto.LIST -> OrderActivityFilterByItemDto.Types.LIST
                        ActivityFilterByItemTypeDto.MATCH -> OrderActivityFilterByItemDto.Types.MATCH
                        ActivityFilterByItemTypeDto.TRANSFER,
                        ActivityFilterByItemTypeDto.MINT,
                        ActivityFilterByItemTypeDto.BURN -> null
                        ActivityFilterByItemTypeDto.CANCEL_BID -> null
                        ActivityFilterByItemTypeDto.CANCEL_LIST -> null
                    }
                }
                if (orderTypes.isNotEmpty()) OrderActivityFilterByItemDto(
                    source.contract,
                    source.tokenId,
                    orderTypes
                ) else null
            }
            is ActivityFilterByUserDto -> {
                val nftTypes = source.types.mapNotNull { type ->
                    when (type) {
                        ActivityFilterByUserTypeDto.MAKE_BID -> OrderActivityFilterByUserDto.Types.MAKE_BID
                        ActivityFilterByUserTypeDto.GET_BID -> OrderActivityFilterByUserDto.Types.GET_BID
                        ActivityFilterByUserTypeDto.BUY -> OrderActivityFilterByUserDto.Types.BUY
                        ActivityFilterByUserTypeDto.LIST -> OrderActivityFilterByUserDto.Types.LIST
                        ActivityFilterByUserTypeDto.SELL -> OrderActivityFilterByUserDto.Types.SELL
                        ActivityFilterByUserTypeDto.TRANSFER_FROM,
                        ActivityFilterByUserTypeDto.TRANSFER_TO,
                        ActivityFilterByUserTypeDto.MINT,
                        ActivityFilterByUserTypeDto.BURN -> null
                        ActivityFilterByUserTypeDto.CANCEL_BID -> null
                        ActivityFilterByUserTypeDto.CANCEL_LIST -> null
                    }
                }
                if (nftTypes.isNotEmpty()) OrderActivityFilterByUserDto(source.users, nftTypes, source.from, source.to) else null
            }
            else -> throw IllegalArgumentException("Unexpected activity filter type $javaClass")
        }
    }
}
