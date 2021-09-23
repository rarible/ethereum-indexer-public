package com.rarible.protocol.nft.listener.service.descriptors.event

import com.rarible.ethereum.listener.log.OnLogEventListener
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.nft.core.converters.dto.NftCollectionActivityConverter
import com.rarible.protocol.nft.core.model.CollectionEventType
import com.rarible.protocol.nft.core.producer.ProtocolNftEventPublisher
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactor.mono
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class OnNftCollectionLogEventListener(
    private val eventPublisher: ProtocolNftEventPublisher
) : OnLogEventListener {

    override val topics: List<Word> = CollectionEventType.values().flatMap { it.topic }.toList()

    override fun onLogEvent(logEvent: LogEvent): Mono<Void> = mono {
        val activity = NftCollectionActivityConverter.convert(logEvent)
        if (activity != null) eventPublisher.publish(activity)
    }.then()
}
