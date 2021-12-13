package com.rarible.protocol.nft.core.model

data class CompositeEntityId(
    val itemId: ItemId?,
    val ownershipIds: List<OwnershipId>
) {
    private val _id: ItemId = when {
        itemId != null -> {
            ownershipIds.forEach {
                require(itemId.token == it.token)
                require(itemId.tokenId == it.tokenId)
            }
            itemId
        }
        ownershipIds.isNotEmpty() -> {
            val itemId = ownershipIds.first().let { ItemId(it.token, it.tokenId) }
            require(ownershipIds.all { it.token == itemId.token && it.tokenId == it.tokenId })
            itemId
        }
        else -> {
            throw IllegalArgumentException("CompositeEntityId can't be empty")
        }
    }

    fun itemId(): ItemId? {
        return _id
    }

    override fun equals(other: Any?): Boolean {
        return (other as? CompositeEntityId)?.let { other._id == _id } ?: false
    }

    override fun hashCode(): Int {
        return _id.hashCode()
    }
}
