package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.core.daemon.sequential.ConsumerBatchEventHandler
import com.rarible.protocol.dto.NftItemEventDto
import com.rarible.protocol.nft.core.model.FeatureFlags
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.springframework.stereotype.Component

@Component
class BatchInternalItemHandler(
    private val delegate: InternalItemHandler,
    private val featureFlags: FeatureFlags
) : ConsumerBatchEventHandler<NftItemEventDto> {

    override suspend fun handle(event: List<NftItemEventDto>) = coroutineScope<Unit> {
        event
            .chunked(featureFlags.internalMetaTopicBatchSize)
            .map { itemEvents ->
                itemEvents.map { itemEvent ->
                    async {
                        delegate.handle(itemEvent)
                    }
                }.awaitAll()
            }
            .flatten()
            .lastOrNull()
    }
}