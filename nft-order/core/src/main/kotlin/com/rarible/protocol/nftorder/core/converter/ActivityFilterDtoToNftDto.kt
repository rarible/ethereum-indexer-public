package com.rarible.protocol.nftorder.core.converter

import com.rarible.protocol.dto.*

object ActivityFilterDtoToNftDto {

    fun convert(source: ActivityFilterDto): NftActivityFilterDto? {
        return when (source) {
            is ActivityFilterAllDto -> {
                val nftTypes = source.types.mapNotNull { type ->
                    when (type) {
                        ActivityFilterAllTypeDto.TRANSFER -> NftActivityFilterAllDto.Types.TRANSFER
                        ActivityFilterAllTypeDto.MINT -> NftActivityFilterAllDto.Types.MINT
                        ActivityFilterAllTypeDto.BURN -> NftActivityFilterAllDto.Types.BURN
                        ActivityFilterAllTypeDto.BID,
                        ActivityFilterAllTypeDto.LIST,
                        ActivityFilterAllTypeDto.SELL -> null
                        ActivityFilterAllTypeDto.CANCEL_BID -> null
                        ActivityFilterAllTypeDto.CANCEL_LIST -> null
                    }
                }
                if (nftTypes.isNotEmpty()) NftActivityFilterAllDto(nftTypes) else null
            }
            is ActivityFilterByCollectionDto -> {
                val nftTypes = source.types.mapNotNull { type ->
                    when (type) {
                        ActivityFilterByCollectionTypeDto.TRANSFER -> NftActivityFilterByCollectionDto.Types.TRANSFER
                        ActivityFilterByCollectionTypeDto.MINT -> NftActivityFilterByCollectionDto.Types.MINT
                        ActivityFilterByCollectionTypeDto.BURN -> NftActivityFilterByCollectionDto.Types.BURN
                        ActivityFilterByCollectionTypeDto.BID,
                        ActivityFilterByCollectionTypeDto.LIST,
                        ActivityFilterByCollectionTypeDto.MATCH -> null
                        ActivityFilterByCollectionTypeDto.CANCEL_BID -> null
                        ActivityFilterByCollectionTypeDto.CANCEL_LIST -> null
                    }
                }
                if (nftTypes.isNotEmpty()) NftActivityFilterByCollectionDto(source.contract, nftTypes) else null
            }
            is ActivityFilterByItemDto -> {
                val nftTypes = source.types.mapNotNull { type ->
                    when (type) {
                        ActivityFilterByItemTypeDto.TRANSFER -> NftActivityFilterByItemDto.Types.TRANSFER
                        ActivityFilterByItemTypeDto.MINT -> NftActivityFilterByItemDto.Types.MINT
                        ActivityFilterByItemTypeDto.BURN -> NftActivityFilterByItemDto.Types.BURN
                        ActivityFilterByItemTypeDto.BID,
                        ActivityFilterByItemTypeDto.LIST,
                        ActivityFilterByItemTypeDto.MATCH -> null
                        ActivityFilterByItemTypeDto.CANCEL_BID -> null
                        ActivityFilterByItemTypeDto.CANCEL_LIST -> null
                    }
                }
                if (nftTypes.isNotEmpty()) NftActivityFilterByItemDto(
                    source.contract,
                    source.tokenId,
                    nftTypes
                ) else null
            }
            is ActivityFilterByUserDto -> {
                val nftTypes = source.types.mapNotNull { type ->
                    when (type) {
                        ActivityFilterByUserTypeDto.TRANSFER_FROM -> NftActivityFilterByUserDto.Types.TRANSFER_FROM
                        ActivityFilterByUserTypeDto.TRANSFER_TO -> NftActivityFilterByUserDto.Types.TRANSFER_TO
                        ActivityFilterByUserTypeDto.MINT -> NftActivityFilterByUserDto.Types.MINT
                        ActivityFilterByUserTypeDto.BURN -> NftActivityFilterByUserDto.Types.BURN
                        ActivityFilterByUserTypeDto.MAKE_BID,
                        ActivityFilterByUserTypeDto.GET_BID,
                        ActivityFilterByUserTypeDto.BUY,
                        ActivityFilterByUserTypeDto.LIST,
                        ActivityFilterByUserTypeDto.SELL -> null
                        ActivityFilterByUserTypeDto.CANCEL_BID -> null
                        ActivityFilterByUserTypeDto.CANCEL_LIST -> null
                    }
                }
                if (nftTypes.isNotEmpty()) NftActivityFilterByUserDto(source.users, nftTypes, source.from, source.to) else null
            }
            else -> throw IllegalArgumentException("Unexpected activity filter type $javaClass")
        }
    }
}
