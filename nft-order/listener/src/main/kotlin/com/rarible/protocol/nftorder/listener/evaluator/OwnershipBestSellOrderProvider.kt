package com.rarible.protocol.nftorder.listener.evaluator

import com.rarible.protocol.dto.OrderDto
import com.rarible.protocol.nftorder.core.model.Ownership
import com.rarible.protocol.nftorder.core.model.OwnershipId
import com.rarible.protocol.nftorder.core.service.OrderService

class OwnershipBestSellOrderProvider(
    private val ownershipId: OwnershipId,
    private val orderService: OrderService
) : BestOrderProvider<Ownership, OwnershipId> {

    override val entityId: OwnershipId = ownershipId
    override val entityType: Class<Ownership> get() = Ownership::class.java

    override suspend fun fetch(): OrderDto? {
        return orderService.getBestSell(ownershipId)
    }
}