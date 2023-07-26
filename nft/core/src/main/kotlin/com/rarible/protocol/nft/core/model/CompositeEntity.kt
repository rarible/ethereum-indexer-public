package com.rarible.protocol.nft.core.model

import com.rarible.core.entity.reducer.model.Identifiable
import scalether.domain.Address

data class CompositeEntity(
    override val id: ItemId,
    val item: Item?,
    val ownerships: MutableMap<Address, Ownership>,
    val firstEvent: CompositeEvent? = null
) : Identifiable<ItemId> {
    constructor(id: ItemId) : this(id, null, mutableMapOf(), null)
    constructor(item: Item) : this(item.id, item, mutableMapOf(), null)

    override val version: Long? = null
}
