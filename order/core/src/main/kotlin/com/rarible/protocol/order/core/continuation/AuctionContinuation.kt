package com.rarible.protocol.order.core.continuation

import com.rarible.protocol.dto.AuctionDto
import com.rarible.protocol.order.core.model.AuctionBidEntityDto

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

    object ByBidValueAndId : ContinuationFactory<AuctionBidEntityDto, PriceIdContinuation> {
        override fun getContinuation(entity: AuctionBidEntityDto): PriceIdContinuation {
            return PriceIdContinuation(entity.dto.amount, entity.id)
        }
    }

    object ByBuyUsdPriceAndId : ContinuationFactory<AuctionDto, PriceIdContinuation> {
        override fun getContinuation(entity: AuctionDto): PriceIdContinuation {
            return PriceIdContinuation(entity.buyPriceUsd, entity.hash.prefixed())
        }
    }
}
