package com.rarible.protocol.nftorder.api.subscriber.autoconfigure

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

internal const val PROTOCOL_NFT_ORDER_SUBSCRIBER = "protocol.nft-order.subscriber"

@ConfigurationProperties(PROTOCOL_NFT_ORDER_SUBSCRIBER)
@ConstructorBinding
data class NftOrderEventsSubscriberProperties(
    val brokerReplicaSet: String
)
