package com.rarible.protocol.nft.core.model

import com.rarible.core.entity.reducer.model.Identifiable

data class CompositeEntity(
    val item: Item?,
    val ownerships: List<Ownership>
) : Identifiable<CompositeEntityId> {
    private val _id: CompositeEntityId = CompositeEntityId(item?.id, ownerships.map { it.id })

    override val id = _id
}

