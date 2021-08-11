package com.rarible.protocol.nft.listener.service.descriptors.event

import com.rarible.contracts.erc1155.TransferBatchEvent
import com.rarible.contracts.erc1155.TransferSingleEvent
import com.rarible.contracts.erc721.TransferEvent
import com.rarible.ethereum.listener.log.OnLogEventListener
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.nft.core.converters.dto.NftActivityConverter
import com.rarible.protocol.nft.core.producer.ProtocolNftEventPublisher
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactor.mono
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class OnNftItemLogEventListener(
    private val eventPublisher: ProtocolNftEventPublisher
) : OnLogEventListener {

    override val topics: List<Word> = listOf(
        TransferSingleEvent.id(),
        TransferBatchEvent.id(),
        TransferEvent.id()
    )

    override fun onLogEvent(logEvent: LogEvent): Mono<Void> = mono {
        val activity = NftActivityConverter.convert(logEvent)
        if (activity != null) eventPublisher.publish(activity)
    }.then()
}