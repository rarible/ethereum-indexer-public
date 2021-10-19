package com.rarible.protocol.nft.core.model

import com.rarible.ethereum.listener.log.domain.EventData
import com.rarible.protocol.contracts.collection.*
import com.rarible.protocol.contracts.erc721.OwnershipTransferredEvent
import io.daonomic.rpc.domain.Word
import scalether.domain.Address

enum class CollectionEventType(val topic: Set<Word>) {
    CREATE(
        setOf(
            CreateERC1155_v1Event.id(),
            CreateEvent.id(),
            CreateERC721_v4Event.id(),
            CreateERC721RaribleUserEvent.id(),
            CreateERC721RaribleEvent.id(),
            CreateERC1155RaribleUserEvent.id(),
            CreateERC1155RaribleEvent.id()
        )
    ),
    OWNERSHIP(
        setOf(OwnershipTransferredEvent.id())
    )
}

sealed class CollectionEvent(var type: CollectionEventType) : EventData {
    abstract val id: Address
}

data class CreateCollection(
    override val id: Address,
    val owner: Address,
    val name: String,
    val symbol: String
) : CollectionEvent(CollectionEventType.CREATE)

data class CollectionOwnershipTransferred(
    override val id: Address,
    val previousOwner: Address,
    val newOwner: Address
) : CollectionEvent(CollectionEventType.OWNERSHIP)
