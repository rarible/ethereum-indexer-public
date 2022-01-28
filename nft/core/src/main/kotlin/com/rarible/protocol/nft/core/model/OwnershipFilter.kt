package com.rarible.protocol.nft.core.model

import scalether.domain.Address
import java.math.BigInteger

sealed class OwnershipFilter {
    abstract val sort: Sort
    abstract val showDeleted: Boolean

    enum class Sort {
        LAST_UPDATE
    }
}

data class OwnershipFilterAll(
    override val sort: Sort,
    override val showDeleted: Boolean
) : OwnershipFilter()

data class OwnershipFilterByOwner(
    override val sort: Sort,
    val owner: Address
) : OwnershipFilter() {
    override val showDeleted: Boolean = false
}

data class OwnershipFilterByCreator(
    override val sort: Sort,
    val creator: Address
) : OwnershipFilter() {
    override val showDeleted: Boolean = false
}

data class OwnershipFilterByCollection(
    override val sort: Sort,
    val collection: Address
) : OwnershipFilter() {
    override val showDeleted: Boolean = false
}

data class OwnershipFilterByItem(
    override val sort: Sort,
    val contract: Address,
    val tokenId: BigInteger
) : OwnershipFilter() {
    override val showDeleted: Boolean = false
}
