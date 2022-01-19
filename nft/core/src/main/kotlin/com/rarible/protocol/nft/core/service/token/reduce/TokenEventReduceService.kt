package com.rarible.protocol.nft.core.service.token.reduce

import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.blockchain.scanner.framework.data.LogRecordEvent
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.entity.reducer.service.EventReduceService
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.converters.model.TokenEventConverter
import com.rarible.protocol.nft.core.model.EntityEventListeners
import com.rarible.protocol.nft.core.model.SubscriberGroup
import com.rarible.protocol.nft.core.model.SubscriberGroups
import com.rarible.protocol.nft.core.service.EntityEventListener
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

    override val groupId: SubscriberGroup = SubscriberGroups.TOKEN_HISTORY

    override suspend fun onEntityEvents(events: List<LogRecordEvent<ReversedEthereumLogRecord>>) {
        events
            .mapNotNull { TokenEventConverter.convert(it.record) }
            .let { delegate.reduceAll(it) }
    }
}
