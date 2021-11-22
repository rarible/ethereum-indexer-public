package com.rarible.protocol.order.core.continuation

import com.rarible.protocol.dto.AuctionBidDto
import com.rarible.protocol.dto.AuctionDto
import io.daonomic.rpc.domain.Word

object AuctionContinuation {
    object ByLastUpdatedAndId : ContinuationFactory<AuctionDto, DateIdContinuation> {
        override fun getContinuation(entity: AuctionDto): DateIdContinuation {
            return DateIdContinuation(entity.lastUpdateAt, entity.hash.prefixed())
        }
    }

    object ByBuyPriceAndId : ContinuationFactory<AuctionDto, PriceIdContinuation> {
        override fun getContinuation(entity: AuctionDto): PriceIdContinuation {
            return PriceIdContinuation(entity.buyPrice, entity.hash.prefixed())
        }
    }

    object ByBidValueAndId : ContinuationFactory<ByBidValueAndId.AuctionBidEntityDto, PriceIdContinuation> {
        override fun getContinuation(entity: AuctionBidEntityDto): PriceIdContinuation {
            return PriceIdContinuation(entity.bidDto.amount, entity.hash.prefixed())
        }

        class AuctionBidEntityDto(
            val hash: Word,
            val bidDto: AuctionBidDto
        )
    }

    object ByBuyUsdPriceAndId : ContinuationFactory<AuctionDto, PriceIdContinuation> {
        override fun getContinuation(entity: AuctionDto): PriceIdContinuation {
            return PriceIdContinuation(entity.buyPriceUsd, entity.hash.prefixed())
        }
    }
}
