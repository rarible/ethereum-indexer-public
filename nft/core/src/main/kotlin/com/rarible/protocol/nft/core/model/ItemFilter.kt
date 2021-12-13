package com.rarible.protocol.nft.core.model

import scalether.domain.Address
import java.time.Instant

sealed class ItemFilter {
    abstract val sort: Sort

    enum class Sort {
        LAST_UPDATE
    }

}

data class ItemFilterAll(
    override val sort: Sort,
    val showDeleted: Boolean,
    val lastUpdatedFrom: Instant? = null
) : ItemFilter()

data class ItemFilterByOwner(
    override val sort: Sort,
    val owner: Address
) : ItemFilter()

data class ItemFilterByCreator(
    override val sort: Sort,
    val creator: Address
) : ItemFilter()

data class ItemFilterByCollection(
    override val sort: Sort,
    val collection: Address,
    val owner: Address? = null
) : ItemFilter()
