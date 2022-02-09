package com.rarible.protocol.nft.core.model

import com.rarible.core.entity.reducer.model.Identifiable
import scalether.domain.Address

data class CompositeEntity(
    override val id: ItemId,
    val item: Item?,
    val ownerships: MutableMap<Address, Ownership>,
) : Identifiable<ItemId>
