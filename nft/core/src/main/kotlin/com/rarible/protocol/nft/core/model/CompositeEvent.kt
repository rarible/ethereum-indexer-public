package com.rarible.protocol.nft.core.model

import com.rarible.blockchain.scanner.ethereum.model.EthereumEntityEvent

data class CompositeEvent(
    val itemEvent: ItemEvent?,
    val ownershipEvents: List<OwnershipEvent>
) : Comparable<CompositeEvent> {
    constructor(itemEvent: ItemEvent) : this(itemEvent, emptyList())
    constructor(ownershipEvents: List<OwnershipEvent>) : this(null, ownershipEvents)

    fun isConfirmed(): Boolean {
        return getEventLog(this).isConfirmed()
    }

    override fun compareTo(other: CompositeEvent): Int {
        val event = getEventLog(this)
        val otherEvent = getEventLog(other)
        return event.compareTo(otherEvent)
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        private fun getEventLog(event: CompositeEvent): EthereumEntityEvent<Any> {
            val itemEvent = event.itemEvent
            val ownershipEvent = event.ownershipEvents.firstOrNull()
            return (itemEvent ?: ownershipEvent)
                ?.let { it as EthereumEntityEvent<Any> }
                ?: throw IllegalStateException("Empty composite event $event")
        }
    }
}
