package com.rarible.protocol.nft.core.service.token.reduce

import com.rarible.blockchain.scanner.ethereum.reduce.EntityEventListener
import com.rarible.blockchain.scanner.framework.data.LogRecordEvent
import com.rarible.core.apm.withTransaction
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.common.nowMillis
import com.rarible.core.entity.reducer.service.EventReduceService
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.converters.model.TokenEventConverter
import com.rarible.protocol.nft.core.misc.addIndexerIn
import com.rarible.protocol.nft.core.misc.asEthereumLogRecord
import com.rarible.protocol.nft.core.model.EntityEventListeners
import com.rarible.protocol.nft.core.model.SubscriberGroup
import com.rarible.protocol.nft.core.model.SubscriberGroups
import org.springframework.stereotype.Component

@Component
class TokenEventReduceService(
    entityService: TokenUpdateService,
    entityIdService: TokenIdService,
    templateProvider: TokenTemplateProvider,
    reducer: TokenReducer,
    properties: NftIndexerProperties,
    environmentInfo: ApplicationEnvironmentInfo
) : EntityEventListener {
    private val delegate = EventReduceService(entityService, entityIdService, templateProvider, reducer)

    override val id: String = EntityEventListeners.tokenHistoryListenerId(environmentInfo.name, properties.blockchain)

    override val subscriberGroup: SubscriberGroup = SubscriberGroups.TOKEN_HISTORY

    override suspend fun onEntityEvents(events: List<LogRecordEvent>) {
        val now = nowMillis()
        withTransaction("onTokenEvents", labels = listOf("size" to events.size)) {
            events
                .mapNotNull {
                    TokenEventConverter.convert(
                        it.record.asEthereumLogRecord(),
                        it.eventTimeMarks?.addIndexerIn(now)
                    )
                }
                .let { delegate.reduceAll(it) }
        }
    }
}
