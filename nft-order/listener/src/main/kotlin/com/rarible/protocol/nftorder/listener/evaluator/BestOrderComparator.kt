package com.rarible.protocol.nftorder.listener.evaluator

import com.rarible.protocol.dto.OrderDto

interface BestOrderComparator {

    val name: String

    fun compare(current: OrderDto, updated: OrderDto): OrderDto


}