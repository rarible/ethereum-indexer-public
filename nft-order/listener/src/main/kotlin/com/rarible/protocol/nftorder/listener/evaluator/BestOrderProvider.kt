package com.rarible.protocol.nftorder.listener.evaluator

import com.rarible.protocol.dto.OrderDto

interface BestOrderProvider<E> {

    val entityId: String
    val entityType: Class<E>

    suspend fun fetch(): OrderDto?

}