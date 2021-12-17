package com.rarible.protocol.nft.core.service

import com.rarible.core.entity.reducer.model.Entity
import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.protocol.nft.core.model.EthereumEntityEvent
import org.slf4j.LoggerFactory

class LoggingReducer<Id, Event : EthereumEntityEvent<Event>, E : Entity<Id, Event, E>> : Reducer<Event, E> {
    override suspend fun reduce(entity: E, event: Event): E {
        val log = event.log

        logger.info(
            "event: {}, status: {}, block: {}, logEvent: {}, minorLogIndex: {}, id: {}",
            event::class.java.simpleName, log.status, log.blockNumber, log.logIndex, log.minorLogIndex, entity.id
        )
        return entity
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LoggingReducer::class.java)
    }
}
