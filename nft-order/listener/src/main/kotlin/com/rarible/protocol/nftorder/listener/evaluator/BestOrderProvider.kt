package com.rarible.protocol.nftorder.listener.evaluator

import com.rarible.protocol.dto.OrderDto

interface BestOrderProvider<E, I> {

    val entityId: I
    val entityType: Class<E>

    suspend fun fetch(): OrderDto?

}