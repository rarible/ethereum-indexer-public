package com.rarible.protocol.nft.core.service.ownership.reduce

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemEvent
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.OwnershipEvent
import com.rarible.protocol.nft.core.service.ownership.reduce.lazy.LazyOwnershipReducer
import com.rarible.protocol.nft.core.service.ownership.reduce.status.EventStatusOwnershipReducer
import com.rarible.protocol.nft.core.service.ownership.reduce.status.OwnershipDeleteReducer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class OwnershipReducer(
    eventStatusOwnershipReducer: EventStatusOwnershipReducer,
    lazyOwnershipReducer: LazyOwnershipReducer
) : Reducer<OwnershipEvent, Ownership> {
    private val eventStatusOwnershipReducer = OwnershipDeleteReducer.wrap(eventStatusOwnershipReducer)
    private val lazyOwnershipReducer = OwnershipDeleteReducer.wrap(lazyOwnershipReducer)

    override suspend fun reduce(entity: Ownership, event: OwnershipEvent): Ownership {
        logEvent(entity, event)

        return when (event) {
            is OwnershipEvent.TransferFromEvent,
            is OwnershipEvent.TransferToEvent,
            is OwnershipEvent.ChangeLazyValueEvent -> {
                eventStatusOwnershipReducer.reduce(entity, event)
            }
            is OwnershipEvent.LazyTransferToEvent -> {
                lazyOwnershipReducer.reduce(entity, event)
            }
        }
    }

    private fun logEvent(entity: Ownership, event: OwnershipEvent) {
        val log = event.log
        logger.info(
            "Ownership: {}, event: {}, status: {}, block: {}, logEvent: {}, minorLogIndex: {}",
            entity.id, event::class.java.simpleName, log.status, log.blockNumber, log.logIndex, log.minorLogIndex
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OwnershipReducer::class.java)
    }
}
