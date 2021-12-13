package com.rarible.protocol.nft.core.model

import com.rarible.core.entity.reducer.model.Identifiable

data class CompositeEntity(
    override val id: ItemId,
    val item: Item?,
    val ownerships: List<Ownership>
) : Identifiable<ItemId>
