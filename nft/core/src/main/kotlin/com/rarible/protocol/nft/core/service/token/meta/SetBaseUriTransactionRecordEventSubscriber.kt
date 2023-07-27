package com.rarible.protocol.nft.core.service.token.meta

import com.rarible.blockchain.scanner.framework.data.TransactionRecordEvent
import com.rarible.blockchain.scanner.framework.listener.TransactionRecordEventSubscriber
import com.rarible.protocol.dto.NftCollectionSetBaseUriEventDto
import com.rarible.protocol.nft.core.misc.addIndexerOut
import com.rarible.protocol.nft.core.misc.toDto
import com.rarible.protocol.nft.core.model.SetBaseUriRecord
import com.rarible.protocol.nft.core.producer.ProtocolNftEventPublisher
import com.rarible.protocol.nft.core.repository.token.TokenRepository
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class SetBaseUriTransactionRecordEventSubscriber(
    private val eventPublisher: ProtocolNftEventPublisher,
    private val tokenRepository: TokenRepository,
) : TransactionRecordEventSubscriber {
    override suspend fun onTransactionRecordEvents(events: List<TransactionRecordEvent>) {
        events.forEach {
            val record = it.record as SetBaseUriRecord
            if (tokenRepository.findById(record.address).awaitSingleOrNull() != null) {
                eventPublisher.publish(
                    NftCollectionSetBaseUriEventDto(
                        eventId = UUID.randomUUID().toString(),
                        id = record.address,
                        eventTimeMarks = it.eventTimeMarks.addIndexerOut().toDto()
                    )
                )
            }
        }
    }
}
