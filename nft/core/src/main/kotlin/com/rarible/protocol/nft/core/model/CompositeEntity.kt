package com.rarible.protocol.nft.core.model

import com.rarible.core.entity.reducer.model.Identifiable

data class CompositeEntity(
    val item: Item?,
    val ownerships: List<Ownership>
) : Identifiable<CompositeEntityId> {
    override val id = CompositeEntityId(item?.id, ownerships.map { it.id })
}

