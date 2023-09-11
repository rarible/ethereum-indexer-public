package com.rarible.protocol.nft.core.service.token.meta

import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.blockchain.scanner.framework.data.LogRecordEvent
import com.rarible.blockchain.scanner.framework.listener.LogRecordEventListener
import com.rarible.protocol.dto.NftItemMetaRefreshEventDto
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.misc.addIndexerIn
import com.rarible.protocol.nft.core.misc.toDto
import com.rarible.protocol.nft.core.model.EntityEventListeners
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.SubscriberGroup
import com.rarible.protocol.nft.core.model.SubscriberGroups
import com.rarible.protocol.nft.core.model.TokenUriReveal
import com.rarible.protocol.nft.core.producer.ProtocolNftEventPublisher
import org.springframework.stereotype.Component
import java.math.BigInteger
import java.time.Instant
import java.util.UUID

@Component
class TokenRevealUriLogRecordEventListener(
    properties: NftIndexerProperties,
    private val itemEventPublisher: ProtocolNftEventPublisher
) : LogRecordEventListener {
    override val id: String = EntityEventListeners.tokenRevealListenerId(properties.blockchain)
    override val groupId: SubscriberGroup = SubscriberGroups.TOKEN_REVEAL

    override suspend fun onLogRecordEvents(events: List<LogRecordEvent>) {
        val now = Instant.now()
        for (event in events) {
            val ethereumLogRecord = event.record as ReversedEthereumLogRecord
            val tokenUriReveal = ethereumLogRecord.data as TokenUriReveal
            var tokenId = tokenUriReveal.tokenIdFrom
            while (tokenId <= tokenUriReveal.tokenIdTo) {
                itemEventPublisher.publish(
                    NftItemMetaRefreshEventDto(
                        eventId = UUID.randomUUID().toString(),
                        itemId = ItemId(tokenUriReveal.contract, tokenId).decimalStringValue,
                        eventTimeMarks = event.eventTimeMarks.addIndexerIn(now).toDto()
                    )
                )
                tokenId += BigInteger.ONE
            }
        }
    }
}
