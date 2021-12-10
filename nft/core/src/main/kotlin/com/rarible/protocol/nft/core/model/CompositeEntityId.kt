package com.rarible.protocol.nft.core.model

data class CompositeEntityId(
    val itemId: ItemId?,
    val ownershipIds: List<OwnershipId>
) {
    init {
        if (itemId != null) {
            ownershipIds.forEach {
                require(itemId.token == it.token)
                require(itemId.tokenId == it.tokenId)
            }
        } else if (ownershipIds.isNotEmpty()) {
            require(ownershipIds.map { ItemId(it.token, it.tokenId) }.toSet().size == 1)
        }
    }

    fun itemId(): ItemId? {
        return itemId ?: ownershipIds.firstOrNull()?.let { ItemId(it.token, it.tokenId) }
    }
}
