package com.rarible.protocol.nft.core.model

data class CompositeEvent(
    val itemEvent: ItemEvent?,
    val ownershipEvents: List<OwnershipEvent>
) : Comparable<CompositeEvent> {
    constructor(itemEvent: ItemEvent): this(itemEvent, emptyList())
    constructor(ownershipEvents: List<OwnershipEvent>): this(null, ownershipEvents)

    override fun compareTo(other: CompositeEvent): Int {
        val event = getEventLog(this)
        val otherEvent = getEventLog(other)
        return EthereumEntityEvent.confirmBlockComparator.compare(event, otherEvent)
    }

    companion object {
        private fun getEventLog(event: CompositeEvent): EthereumEntityEvent<*> {
            val itemEvent = event.itemEvent?.takeIf { it.isConfirmed() }
            val ownershipEvent = event.ownershipEvents.firstOrNull()?.takeIf { it.isConfirmed() }
            return itemEvent ?: ownershipEvent ?: throw IllegalStateException("Empty composite event $event")
        }
    }
}
