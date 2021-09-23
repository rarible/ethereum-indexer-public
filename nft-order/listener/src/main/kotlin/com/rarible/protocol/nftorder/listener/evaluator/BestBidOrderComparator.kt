package com.rarible.protocol.nftorder.listener.evaluator

import com.rarible.protocol.dto.OrderDto

object BestBidOrderComparator : BestOrderComparator {

    override val name: String = "BestBidOrder"

    override fun compare(current: OrderDto, updated: OrderDto): OrderDto {
        val currentTakePrice = current.takePriceUsd
        val updatedTakePrice = updated.takePriceUsd

        val isCurrentTakePriceLesser = when {
            currentTakePrice == null -> true
            updatedTakePrice != null -> currentTakePrice <= updatedTakePrice
            else -> false
        }

        // We have new price, which is higher, then current - updated order is better, using it
        return if (isCurrentTakePriceLesser) updated else current
    }
}