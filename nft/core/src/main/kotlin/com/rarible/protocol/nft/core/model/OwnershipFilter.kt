package com.rarible.protocol.nft.core.model

import scalether.domain.Address
import java.math.BigInteger

sealed class OwnershipFilter {
    abstract val sort: Sort

    enum class Sort {
        LAST_UPDATE
    }

}

data class OwnershipFilterAll(
    override val sort: Sort
) : OwnershipFilter()

data class OwnershipFilterByOwner(
    override val sort: Sort,
    val owner: Address
) : OwnershipFilter()

data class OwnershipFilterByCreator(
    override val sort: Sort,
    val creator: Address
) : OwnershipFilter()

data class OwnershipFilterByCollection(
    override val sort: Sort,
    val collection: Address
) : OwnershipFilter()

data class OwnershipFilterByItem(
    override val sort: Sort,
    val contract: Address,
    val tokenId: BigInteger
) : OwnershipFilter()
