package com.rarible.protocol.nftorder.listener.evaluator

import com.rarible.protocol.dto.OrderDto

object BestBidOrderComparator : BestOrderComparator {

    override val name: String = "BestBidOrder"

    override fun compare(current: OrderDto, updated: OrderDto): OrderDto {
        val currentMakePrice = current.makePriceUsd
        val updatedMakePrice = updated.makePriceUsd

        val isCurrentMakePriceLesser = when {
            currentMakePrice == null -> true
            updatedMakePrice != null -> currentMakePrice <= updatedMakePrice
            else -> false
        }

        // We have new price, which is higher, then current - updated order is better, using it
        return if (isCurrentMakePriceLesser) updated else current
    }
}