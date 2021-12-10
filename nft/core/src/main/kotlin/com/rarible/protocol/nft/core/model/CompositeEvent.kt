package com.rarible.protocol.nft.core.model

data class CompositeEvent(
    val itemEvent: ItemEvent?,
    val ownershipEvents: List<OwnershipEvent>
) : Comparable<CompositeEvent> {

    override fun compareTo(other: CompositeEvent): Int {
        throw UnsupportedOperationException("Must not be called")
    }
}
